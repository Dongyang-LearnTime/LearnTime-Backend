package learntime.backend.domain.study_member.service;

import learntime.backend.domain.relationship.repository.UserBlockRepository;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.converter.StudyMemberConverter;
import learntime.backend.domain.study_member.dto.response.StudyMemberResponseDTO;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study_member.dto.request.ChangeOwnerRequestDTO;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.utils.StudyAuthUtil;
import learntime.backend.global.utils.UserBlockUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class StudyMemberService {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserBlockUtil userBlockUtil;

    private static final int STUDY_MEMBER_LIMIT_COUNT = 4;

    @Transactional(readOnly = true)
    public List<StudyMemberResponseDTO> getAllStudyMember(Long studyId, Long userId) {
        boolean isStudyMember =
                studyMemberRepository.existsByStudy_StudyIdAndUser_UserIdAndStatus(
                        studyId,
                        userId,
                        StudyMemberStatus.ACTIVE
                );
        if (!isStudyMember) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND);
        }

        List<StudyMember> studyMemberList = studyMemberRepository.findAllActiveByStudyIdFetchUser(studyId);

        Set<Long> blockedIds = userBlockRepository.findBlockedUserIds(userId);

        return studyMemberList.stream()
                .map(studyMember -> StudyMemberConverter.toStudyMemberResponseDTO(
                                studyMember,
                                blockedIds
                        )
                )
                .toList();
    }

    @Transactional
    public void changeStudyOwner(ChangeOwnerRequestDTO request, Long ownerId) {
        // 현재 OWNER 멤버 조회
        StudyMember ownerStudyMember = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserIdAndStatus(request.studyId(), ownerId, StudyMemberStatus.ACTIVE)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        // 새 OWNER 멤버 조회
        StudyMember newOwnerStudyMember = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserId(request.studyId(), request.newOwnerMemberId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        if (!newOwnerStudyMember.isActive()) {
            throw new StudyException(StudyErrorCode.INACTIVE_STUDY_MEMBER);
        }

        // 방장과 바뀔 맴버가 동일 인물인지 확인
        if (ownerId.equals(newOwnerStudyMember.getUser().getUserId())) {
            throw new StudyException(StudyErrorCode.INVALID_OWNER_TRANSFER);
        }

        StudyAuthUtil.checkOwnerRole(ownerStudyMember); // 요청자가 방장인지 확인

        ownerStudyMember.changeRole(StudyMemberRole.MEMBER);
        newOwnerStudyMember.changeRole(StudyMemberRole.OWNER);
    }

    @Transactional
    public void kickStudyMember(Long studyId, Long memberIdToKick, Long requesterId) {
        // requester 검증 (방장인지)
        StudyMember requester = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, requesterId, StudyMemberStatus.ACTIVE)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        StudyAuthUtil.checkOwnerRole(requester);

        // kick 대상 검증
        StudyMember memberToKick = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, memberIdToKick, StudyMemberStatus.ACTIVE)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        if (memberToKick.getStudyMemberRole() == StudyMemberRole.OWNER) {
            throw new StudyException(StudyErrorCode.CANNOT_KICK_OWNER);
        }

        memberToKick.withdraw();
    }

    @Transactional
    public void leaveStudy(Long studyId, Long userId) {
        StudyMember studyMember = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        if (studyMember.getStudyMemberRole() == StudyMemberRole.OWNER) {
            throw new StudyException(StudyErrorCode.OWNER_LEAVE_NOT_ALLOWED);
        }

        studyMember.withdraw();
    }

    @Transactional
    public Long joinPublicStudy(Long studyId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 1. 비관적 락으로 스터디 조회
        Study study = studyRepository.findByIdWithPessimisticLock(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        // 2. 공개 스터디 여부 검증
        if (!Boolean.TRUE.equals(study.getIsPublic())) {
            throw new StudyException(StudyErrorCode.STUDY_NOT_PUBLIC);
        }

        // 3. 정원 검증 (최대 4명)
        long activeMemberCount = studyMemberRepository.countByStudyAndStatusIn(
                study,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)
        );
        if (activeMemberCount >= STUDY_MEMBER_LIMIT_COUNT) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED);
        }

        // 4. 이미 참여 중인지 검증
        boolean alreadyMember = studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, userId);
        if (alreadyMember) {
            throw new StudyException(StudyErrorCode.ALREADY_STUDY_MEMBER);
        }

        // 5. 방장과의 차단 여부 검증
        StudyMember owner = studyMemberRepository
                .findByStudy_StudyIdAndStudyMemberRoleAndStatus(studyId, StudyMemberRole.OWNER, StudyMemberStatus.ACTIVE)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
        userBlockUtil.validateNotBlockedByUser(owner.getUser().getUserId(), userId);

        // 6. 스터디 멤버 저장
        StudyMember newMember = StudyMember.builder()
                .study(study)
                .user(user)
                .studyMemberRole(StudyMemberRole.MEMBER)
                .build();

        StudyMember savedMember = studyMemberRepository.save(newMember);
        return savedMember.getStudyMemberId();
    }

}
