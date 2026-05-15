package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.dto.event.StudyParticipantLeftEvent;
import learntime.backend.domain.study.dto.event.StudySharedEvent;
import learntime.backend.domain.study.dto.response.SharedStudyResponseDTO;
import learntime.backend.domain.study.dto.response.StudyParticipantResponseDTO;
import learntime.backend.domain.study.enums.StudyParticipantRole;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyParticipant;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyParticipantRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.FriendRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
// 공부 일정 공유 참가자 생성, 친구 검증, 나가기 처리를 담당하는 Service
public class StudyShareService {

    private static final int MAX_SHARED_FRIEND_COUNT = 3;

    private final StudyParticipantRepository studyParticipantRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 방장(소유자)을 스터디 참가자로 등록합니다.
     */
    @Transactional
    public StudyParticipant createOwnerParticipant(Study study, User owner) {
        StudyParticipant participant = StudyParticipant.builder()
                .study(study)
                .user(owner)
                .role(StudyParticipantRole.OWNER)
                .build();
        return studyParticipantRepository.save(participant);
    }

    /**
     * 스터디 생성 시 공유 대상(친구들)의 유효성을 검증합니다.
     */
    @Transactional(readOnly = true)
    public void validateShareTargetsForCreate(Long ownerId, List<Long> sharedFriendIds) {
        if (sharedFriendIds == null || sharedFriendIds.isEmpty()) {
            return;
        }

        // 인원수 제한, 중복 및 방장 포함 여부 검증
        validateBasicShareTargets(ownerId, sharedFriendIds);

        // 공유 대상은 반드시 생성자의 친구여야 합니다.
        for (Long friendId : sharedFriendIds) {
            if (!friendRepository.existsFriendship(ownerId, friendId)) {
                throw new StudyException(StudyErrorCode.STUDY_SHARE_ONLY_FRIEND);
            }
        }
    }

    /**
     * 기존 스터디를 친구들과 공유하고 일일 계획을 복제하여 할당합니다.
     */
    @Transactional
    public void shareStudyWithFriends(Study study, Long ownerId, List<Long> sharedFriendIds) {
        if (sharedFriendIds == null || sharedFriendIds.isEmpty()) {
            return;
        }

        // 공유 대상 유효성 및 기존 참가자 여부 검증
        validateShareTargetsForExistingStudy(study.getStudyId(), ownerId, sharedFriendIds);

        List<StudyDailyPlan> ownerPlans = studyDailyPlanRepository.findByStudyOrderByDayNumberAsc(study);
        User owner = study.getUser();

        for (Long friendId : sharedFriendIds) {
            User friend = userRepository.findById(friendId)
                    .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

            // 친구를 즉시 참가자로 등록합니다.
            StudyParticipant member = studyParticipantRepository.save(StudyParticipant.builder()
                    .study(study)
                    .user(friend)
                    .role(StudyParticipantRole.MEMBER)
                    .build());

            // 진행률이 독립되도록 owner의 일일 계획을 친구 참가자용으로 복제합니다.
            List<StudyDailyPlan> copiedPlans = ownerPlans.stream()
                    .map(plan -> StudyDailyPlan.builder()
                            .study(study)
                            .studyParticipant(member)
                            .dayNumber(plan.getDayNumber())
                            .planDate(plan.getPlanDate())
                            .planContent(plan.getPlanContent())
                            .build())
                    .toList();
            studyDailyPlanRepository.saveAll(copiedPlans);

            // 스터디 공유 이벤트 발행
            eventPublisher.publishEvent(new StudySharedEvent(
                    study.getStudyId(),
                    study.getStudyTitle(),
                    owner.getUserId(),
                    owner.getName(),
                    friend.getUserId()
            ));
        }
    }

    /**
     * 특정 스터디에 참여 중인 전체 참가자 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<StudyParticipantResponseDTO> getParticipants(Long studyId, Long userId) {
        // 조회자가 해당 스터디의 활성 참가자인지 확인
        getActiveParticipant(studyId, userId);

        return studyParticipantRepository.findAllByStudy_StudyIdAndLeftAtIsNullOrderByCreatedAtAsc(studyId)
                .stream()
                .map(StudyParticipantResponseDTO::from)
                .toList();
    }

    /**
     * 사용자가 참여 중인 공유 스터디 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<SharedStudyResponseDTO> getMySharedStudies(Long userId) {
        return studyParticipantRepository.findAllByUser_UserIdAndLeftAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(SharedStudyResponseDTO::from)
                .toList();
    }

    /**
     * 참가자가 공유 스터디에서 나갑니다.
     */
    @Transactional
    public void leaveStudy(Long studyId, Long userId) {
        StudyParticipant participant = getActiveParticipant(studyId, userId);

        // 방장은 스터디에서 나갈 수 없음
        if (participant.isOwner()) {
            throw new StudyException(StudyErrorCode.STUDY_OWNER_CANNOT_LEAVE);
        }

        // 과거 기록은 보존하고 현재 참여 목록에서만 제외 처리합니다.
        participant.leave();

        Study study = participant.getStudy();
        // 스터디 나가기 이벤트 발행
        eventPublisher.publishEvent(new StudyParticipantLeftEvent(
                study.getStudyId(),
                study.getStudyTitle(),
                study.getUser().getUserId(),
                participant.getUser().getUserId(),
                participant.getUser().getName()
        ));
    }

    /**
     * 활성화된(나가지 않은) 참가자 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public StudyParticipant getActiveParticipant(Long studyId, Long userId) {
        return studyParticipantRepository.findByStudy_StudyIdAndUser_UserIdAndLeftAtIsNull(studyId, userId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_PARTICIPANT_NOT_FOUND));
    }

    /**
     * 인원수 제한, 중복, 방장 포함 여부 등 기본적인 대상을 검증합니다.
     */
    private void validateBasicShareTargets(Long ownerId, List<Long> sharedFriendIds) {
        if (sharedFriendIds.size() > MAX_SHARED_FRIEND_COUNT) {
            throw new StudyException(StudyErrorCode.STUDY_SHARE_LIMIT_EXCEEDED);
        }

        Set<Long> uniqueIds = new HashSet<>(sharedFriendIds);
        if (uniqueIds.size() != sharedFriendIds.size() || uniqueIds.contains(ownerId)) {
            throw new StudyException(StudyErrorCode.STUDY_SHARE_DUPLICATED_USER);
        }
    }

    /**
     * 기존 스터디 공유 시 인원수 초과, 친구 여부, 중복 참여 여부를 검증합니다.
     */
    private void validateShareTargetsForExistingStudy(Long studyId, Long ownerId, List<Long> sharedFriendIds) {
        validateBasicShareTargets(ownerId, sharedFriendIds);

        long currentMemberCount = studyParticipantRepository.countByStudy_StudyIdAndRoleAndLeftAtIsNull(
                studyId,
                StudyParticipantRole.MEMBER
        );
        if (currentMemberCount + sharedFriendIds.size() > MAX_SHARED_FRIEND_COUNT) {
            throw new StudyException(StudyErrorCode.STUDY_SHARE_LIMIT_EXCEEDED);
        }

        for (Long friendId : sharedFriendIds) {
            if (!friendRepository.existsFriendship(ownerId, friendId)) {
                throw new StudyException(StudyErrorCode.STUDY_SHARE_ONLY_FRIEND);
            }
            if (studyParticipantRepository.existsByStudy_StudyIdAndUser_UserId(studyId, friendId)) {
                throw new StudyException(StudyErrorCode.STUDY_SHARE_DUPLICATED_USER);
            }
        }
    }
}
