package learntime.backend.domain.study_member.service;

import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.converter.StudyJoinRequestConverter;
import learntime.backend.domain.study_member.dto.response.StudyJoinRequestResponseDTO;
import learntime.backend.domain.study_member.enums.StudyJoinRequestStatus;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.event.StudyJoinRequestApprovedEvent;
import learntime.backend.domain.study_member.event.StudyJoinRequestCreatedEvent;
import learntime.backend.domain.study_member.event.StudyJoinRequestRejectedEvent;
import learntime.backend.domain.study_member.model.StudyJoinRequest;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyJoinRequestRepository;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.utils.StudyAuthUtil;
import learntime.backend.global.utils.UserBlockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyJoinRequestService {

    private static final int STUDY_MEMBER_LIMIT_COUNT = 4;

    private final StudyJoinRequestRepository studyJoinRequestRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;
    private final UserBlockUtil userBlockUtil;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 공개 스터디에 가입 요청 등록 (지원자)
     */
    @Transactional
    public Long requestJoin(Long studyId, Long userId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        // 1. 공개 스터디 여부 검증
        if (!Boolean.TRUE.equals(study.getIsPublic())) {
            throw new StudyException(StudyErrorCode.STUDY_NOT_PUBLIC);
        }

        // 2. 정원 검증
        long currentCount = studyMemberRepository.countByStudyAndStatusIn(
                study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)
        );
        if (currentCount >= STUDY_MEMBER_LIMIT_COUNT) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED);
        }

        // 3. 이미 스터디 멤버인지 검증
        if (studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, userId)) {
            throw new StudyException(StudyErrorCode.ALREADY_STUDY_MEMBER);
        }

        // 4. 이미 대기 중인 가입 요청이 있는지 검증
        if (studyJoinRequestRepository.existsByStudy_StudyIdAndRequesterUser_UserIdAndStatus(
                studyId, userId, StudyJoinRequestStatus.PENDING)) {
            throw new StudyException(StudyErrorCode.ALREADY_JOIN_REQUESTED);
        }

        // 5. 방장 조회 및 차단 검증 / 본인 스터디 가입 요청 방지
        StudyMember ownerMember = studyMemberRepository.findByStudy_StudyIdAndStudyMemberRoleAndStatus(
                studyId, StudyMemberRole.OWNER, StudyMemberStatus.ACTIVE
        ).orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        if (ownerMember.getUser().getUserId().equals(userId)) {
            throw new StudyException(StudyErrorCode.SELF_JOIN_REQUEST_NOT_ALLOWED);
        }

        userBlockUtil.validateNotBlockedByUser(ownerMember.getUser().getUserId(), userId);

        // 6. 가입 요청 엔티티 저장
        StudyJoinRequest joinRequest = StudyJoinRequestConverter.toEntity(study, requester);
        StudyJoinRequest savedRequest = studyJoinRequestRepository.save(joinRequest);

        // 7. 방장에게 알림 이벤트 발행
        eventPublisher.publishEvent(new StudyJoinRequestCreatedEvent(
                savedRequest.getStudyJoinRequestId(),
                study.getStudyId(),
                study.getStudyTitle(),
                requester.getName(),
                ownerMember.getUser().getUserId()
        ));

        log.info("[Study Join Request] 스터디 가입 요청 등록 완료 - requestId: {}, studyId: {}, requesterId: {}",
                savedRequest.getStudyJoinRequestId(), studyId, userId);

        return savedRequest.getStudyJoinRequestId();
    }

    /**
     * 스터디 가입 요청 승인 (방장)
     */
    @Transactional
    public Long approveRequest(Long requestId, Long ownerUserId) {
        StudyJoinRequest joinRequest = studyJoinRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_JOIN_REQUEST_NOT_FOUND));

        if (!joinRequest.isPending()) {
            throw new StudyException(StudyErrorCode.JOIN_REQUEST_NOT_PENDING);
        }

        Long studyId = joinRequest.getStudy().getStudyId();

        // 1. 방장 권한 확인
        StudyMember ownerMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(
                studyId, ownerUserId, StudyMemberStatus.ACTIVE
        ).orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
        StudyAuthUtil.checkOwnerRole(ownerMember);

        // 2. 동시성 제어를 위해 비관적 락으로 스터디 조회 및 정원 재검증
        Study lockedStudy = studyRepository.findByIdWithPessimisticLock(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        long currentCount = studyMemberRepository.countByStudyAndStatusIn(
                lockedStudy, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)
        );
        if (currentCount >= STUDY_MEMBER_LIMIT_COUNT) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED);
        }

        User requester = joinRequest.getRequesterUser();
        if (studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, requester.getUserId())) {
            throw new StudyException(StudyErrorCode.ALREADY_STUDY_MEMBER);
        }

        // 3. 요청 승인 처리
        joinRequest.approve();

        // 4. 스터디 멤버로 등록
        StudyMember newMember = StudyMember.builder()
                .study(lockedStudy)
                .user(requester)
                .studyMemberRole(StudyMemberRole.MEMBER)
                .status(StudyMemberStatus.ACTIVE)
                .build();
        StudyMember savedMember = studyMemberRepository.save(newMember);

        // 5. 요청자에게 승인 알림 이벤트 발행
        eventPublisher.publishEvent(new StudyJoinRequestApprovedEvent(
                joinRequest.getStudyJoinRequestId(),
                studyId,
                lockedStudy.getStudyTitle(),
                requester.getUserId()
        ));

        log.info("[Study Join Request] 가입 요청 승인 완료 - requestId: {}, newMemberId: {}",
                requestId, savedMember.getStudyMemberId());

        return savedMember.getStudyMemberId();
    }

    /**
     * 스터디 가입 요청 거절 (방장)
     */
    @Transactional
    public void rejectRequest(Long requestId, Long ownerUserId) {
        StudyJoinRequest joinRequest = studyJoinRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_JOIN_REQUEST_NOT_FOUND));

        if (!joinRequest.isPending()) {
            throw new StudyException(StudyErrorCode.JOIN_REQUEST_NOT_PENDING);
        }

        Long studyId = joinRequest.getStudy().getStudyId();

        // 방장 권한 확인
        StudyMember ownerMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(
                studyId, ownerUserId, StudyMemberStatus.ACTIVE
        ).orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
        StudyAuthUtil.checkOwnerRole(ownerMember);

        joinRequest.reject();

        // 요청자에게 거절 알림 이벤트 발행
        eventPublisher.publishEvent(new StudyJoinRequestRejectedEvent(
                joinRequest.getStudyJoinRequestId(),
                studyId,
                joinRequest.getStudy().getStudyTitle(),
                joinRequest.getRequesterUser().getUserId()
        ));

        log.info("[Study Join Request] 가입 요청 거절 완료 - requestId: {}", requestId);
    }

    /**
     * 스터디 가입 요청 취소 (요청자)
     */
    @Transactional
    public void cancelRequest(Long requestId, Long userId) {
        StudyJoinRequest joinRequest = studyJoinRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_JOIN_REQUEST_NOT_FOUND));

        if (!joinRequest.isPending()) {
            throw new StudyException(StudyErrorCode.JOIN_REQUEST_NOT_PENDING);
        }

        // 요청자 본인 확인
        if (!joinRequest.getRequesterUser().getUserId().equals(userId)) {
            throw new StudyException(StudyErrorCode.NOT_REQUESTER_USER);
        }

        joinRequest.cancel();

        log.info("[Study Join Request] 가입 요청 취소 완료 - requestId: {}, userId: {}", requestId, userId);
    }

    /**
     * 특정 스터디의 대기 중인 가입 요청 목록 조회 (방장 전용)
     */
    @Transactional(readOnly = true)
    public List<StudyJoinRequestResponseDTO> getPendingRequestsForStudy(Long studyId, Long ownerUserId) {
        StudyMember ownerMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(
                studyId, ownerUserId, StudyMemberStatus.ACTIVE
        ).orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
        StudyAuthUtil.checkOwnerRole(ownerMember);

        List<StudyJoinRequest> requests = studyJoinRequestRepository.findAllByStudyIdAndStatusWithDetails(
                studyId, StudyJoinRequestStatus.PENDING
        );

        return requests.stream()
                .map(StudyJoinRequestConverter::toResponseDTO)
                .toList();
    }

    /**
     * 내가 신청한 가입 요청 내역 목록 조회 (요청자 전용)
     */
    @Transactional(readOnly = true)
    public List<StudyJoinRequestResponseDTO> getMyJoinRequests(Long userId) {
        List<StudyJoinRequest> requests = studyJoinRequestRepository.findAllByRequesterUserIdWithStudy(userId);

        return requests.stream()
                .map(StudyJoinRequestConverter::toResponseDTO)
                .toList();
    }
}
