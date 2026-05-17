package learntime.backend.domain.studymember.service;

import jakarta.transaction.Transactional;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.studymember.dto.request.StudyMemberRequestDTO;
import learntime.backend.domain.studymember.enums.StudyInvitationStatus;
import learntime.backend.domain.studymember.event.StudyInvitationSentEvent;
import learntime.backend.domain.studymember.model.StudyInvitation;
import learntime.backend.domain.studymember.model.StudyMember;
import learntime.backend.domain.studymember.repository.StudyMemberRepository;
import learntime.backend.domain.studymember.repository.StudyInvitationRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.FriendRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.utils.StudyAuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudyInvitationService {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyInvitationRepository studyInvitationRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final int STUDY_MEMBER_LIMIT_COUNT = 4;

    // 초대 받은 사용자, 초대한 사용자 순으로 받음
    @Transactional
    public Long inviteMember(StudyMemberRequestDTO request, Long inviterUserId) {
        // 스터디 멤버 조회
        StudyMember studyMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserId(request.studyId(), inviterUserId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        // 초대 대상 유저 조회
        User invitedUser = userRepository.findById(request.invitedUserId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

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

        long memberCount = studyMemberRepository.countByStudy(study);
        if (memberCount >= STUDY_MEMBER_LIMIT_COUNT) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED);
        }

        // 이미 스터디 멤버인지 검증
        boolean alreadyMember = studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(request.studyId(), invitedUser.getUserId());

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

}
