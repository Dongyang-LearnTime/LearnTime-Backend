package learntime.backend.domain.study_member.service;

import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.converter.StudyMemberConverter;
import learntime.backend.domain.study_member.dto.request.StudyMemberRequestDTO;
import learntime.backend.domain.study_member.dto.response.StudyInvitationResponseDTO;
import learntime.backend.domain.study_member.enums.StudyInvitationStatus;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.event.StudyInvitationAcceptedEvent;
import learntime.backend.domain.study_member.event.StudyInvitationRejectedEvent;
import learntime.backend.domain.study_member.event.StudyInvitationSentEvent;
import learntime.backend.domain.study_member.model.StudyInvitation;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study_member.repository.StudyInvitationRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.relationship.repository.FriendRepository;
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

import learntime.backend.domain.study_member.dto.response.StudyMemberFriendResponseDTO;
import learntime.backend.domain.relationship.model.Friend;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyInvitationService {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyInvitationRepository studyInvitationRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final UserBlockUtil userBlockUtil;

    private final int STUDY_MEMBER_LIMIT_COUNT = 4;

    // 받은 초대 목록 반환
    @Transactional(readOnly = true)
    public List<StudyInvitationResponseDTO> getReceivedInvitationList(Long userId) {
        List<StudyInvitation> studyInvitationList =
                studyInvitationRepository.findAllByInvitedUser_UserIdAndStatus(
                        userId,
                        StudyInvitationStatus.PENDING
                );

        return studyInvitationList.stream()
                .map(StudyMemberConverter::toInvitationReceivedResponse)
                .toList();
    }


    // 보낸 초대 목록 반환
    @Transactional(readOnly = true)
    public List<StudyInvitationResponseDTO> getSentInvitationList(Long userId) {
        List<StudyInvitation> studyInvitationList =
                studyInvitationRepository.findAllByInviterUser_UserIdAndStatus(
                        userId,
                        StudyInvitationStatus.PENDING
                );

        return studyInvitationList.stream()
                .map(StudyMemberConverter::toInvitationSentResponse)
                .toList();
    }

    @Transactional
    public void approveRequest(Long invitationId, Long userId) {
        StudyInvitation invitation = validateInvitation(invitationId);
        validateInvitedUser(invitation, userId);
        
        // Study row lock 획득
        Study study = studyRepository
                .findByIdWithPessimisticLock(invitation.getStudy().getStudyId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        long memberCount = studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED));
        if (memberCount >= STUDY_MEMBER_LIMIT_COUNT) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED);
        }

        invitation.accept();
        studyMemberRepository.save(StudyMember.builder()
                .study(study)
                .user(invitation.getInvitedUser())
                .studyMemberRole(StudyMemberRole.MEMBER)
                .build());

        eventPublisher.publishEvent(new StudyInvitationAcceptedEvent(
                invitation.getStudyInvitationId(),
                study.getStudyId(),
                study.getStudyTitle(),
                invitation.getInvitedUser().getName(),
                invitation.getInviterUser().getUserId()
        ));
    }

    @Transactional
    public void rejectRequest(Long invitationId, Long userId) {
        StudyInvitation invitation = validateInvitation(invitationId);
        validateInvitedUser(invitation, userId);

        invitation.reject(); // 초대 거절

        eventPublisher.publishEvent(new StudyInvitationRejectedEvent(
                invitation.getStudyInvitationId(),
                invitation.getStudy().getStudyId(),
                invitation.getStudy().getStudyTitle(),
                invitation.getInvitedUser().getName(),
                invitation.getInviterUser().getUserId()
        ));
    }

    @Transactional
    public void cancelRequest(Long invitationId, Long userId) { // 초대 취소
        StudyInvitation invitation = validateInvitation(invitationId);
        validateInviterUser(invitation, userId);

        invitation.cancel();
    }

    // 초대 받은 사용자, 초대한 사용자 순으로 받음
    @Transactional
    public Long inviteMember(StudyMemberRequestDTO request, Long inviterUserId) {
        // 스터디 멤버 조회
        StudyMember studyMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(
                        request.studyId(),
                        inviterUserId,
                        StudyMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        // 초대 대상 유저 조회
        User invitedUser = userRepository.findById(request.invitedUserId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 차단 당했는지 확인
        userBlockUtil.validateNotBlockedByUser(inviterUserId, request.invitedUserId());

        // OWNER 권한 검증
        StudyAuthUtil.checkOwnerRole(studyMember);

        // 친구 관계 검증
        boolean isFriend = friendRepository.existsFriendRelation(inviterUserId, invitedUser.getUserId());
        if (!isFriend) {
            throw new StudyException(StudyErrorCode.NOT_FRIEND_RELATION);
        }

        // 인원 초과 여부 검증
        // Study row lock 획득
        Study study = studyRepository
                .findByIdWithPessimisticLock(request.studyId())
                .orElseThrow(() ->
                        new StudyException(StudyErrorCode.STUDY_NOT_FOUND)
                );

        long memberCount = studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED));
        if (memberCount >= STUDY_MEMBER_LIMIT_COUNT) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED);
        }

        // 이미 스터디 멤버인지 검증
        boolean alreadyMember = studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(
                request.studyId(),
                invitedUser.getUserId()
        );

        if (alreadyMember) {
            throw new StudyException(StudyErrorCode.ALREADY_STUDY_MEMBER);
        }

        // 이미 초대 중인지 검증
        boolean alreadyInvited = studyInvitationRepository
                        .existsByStudy_StudyIdAndInvitedUser_UserIdAndStatus(
                                request.studyId(),
                                invitedUser.getUserId(),
                                StudyInvitationStatus.PENDING
                        );
        if (alreadyInvited) {
            throw new StudyException(StudyErrorCode.STUDY_INVITATION_ALREADY_EXISTS);
        }

        // 초대 엔티티 생성
        StudyInvitation studyInvitation = StudyInvitation.builder()
                .study(studyMember.getStudy())
                .invitedUser(invitedUser)
                .inviterUser(studyMember.getUser())
                .build();

        StudyInvitation savedStudyInvitation =
                studyInvitationRepository.save(studyInvitation);

        // 알림 생성
        eventPublisher.publishEvent(new StudyInvitationSentEvent(
                savedStudyInvitation.getStudyInvitationId(),
                request.studyId(),
                study.getStudyTitle(),
                studyMember.getUser().getName(),
                request.invitedUserId()
        ));

        return savedStudyInvitation.getStudyInvitationId();
    }

    // 친구 목록과 스터디 초대 여부 조회
    @Transactional(readOnly = true)
    public List<StudyMemberFriendResponseDTO> getFriendsForStudyInvite(Long studyId, Long userId) {
        // 스터디 멤버 및 OWNER 권한 확인
        StudyMember studyMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(
                        studyId,
                        userId,
                        StudyMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        StudyAuthUtil.checkOwnerRole(studyMember);

        // 사용자의 친구 목록 조회
        List<Friend> friends = friendRepository.findAllByUserId(userId);

        // 해당 스터디의 PENDING 초대 목록 조회
        List<StudyInvitation> pendingInvitations = studyInvitationRepository
                .findAllByStudy_StudyIdAndStatusFetchInvitedUser(studyId, StudyInvitationStatus.PENDING);
        Set<Long> pendingInvitedUserIds = pendingInvitations.stream()
                .map(i -> i.getInvitedUser().getUserId())
                .collect(Collectors.toSet());

        // DTO 변환 및 결과 반환
        return friends.stream()
                .map(friend -> StudyMemberConverter.toStudyMemberFriendResponseDTO(
                        friend,
                        userId,
                        pendingInvitedUserIds
                ))
                .toList();
    }

    private void validateInvitedUser(StudyInvitation invitation, Long userId) {
        if (!invitation.getInvitedUser().getUserId().equals(userId)) {
            throw new StudyException(StudyErrorCode.NOT_INVITED_USER);
        }
    }

    private void validateInviterUser(StudyInvitation invitation, Long userId) {
        if (!invitation.getInviterUser().getUserId().equals(userId)) {
            throw new StudyException(StudyErrorCode.NOT_INVITER_USER);
        }
    }

    private StudyInvitation validateInvitation(Long invitationId) {
        StudyInvitation invitation = studyInvitationRepository.findByIdFetchAll(invitationId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_INVITATION_NOT_FOUND));

        if (!invitation.isPending()) {
            throw new StudyException(StudyErrorCode.STUDY_INVITATION_NOT_PENDING);
        }
        return invitation;
    }

}
