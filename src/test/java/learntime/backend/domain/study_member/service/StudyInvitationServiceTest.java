package learntime.backend.domain.study_member.service;

import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.dto.request.StudyMemberRequestDTO;
import learntime.backend.domain.study_member.enums.StudyInvitationStatus;
import learntime.backend.domain.study_member.dto.response.StudyMemberFriendResponseDTO;
import learntime.backend.domain.study_member.model.StudyInvitation;
import learntime.backend.domain.study_member.model.StudyMember;
import java.util.List;
import learntime.backend.domain.study_member.repository.StudyInvitationRepository;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.friend.model.Friend;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.friend.repository.FriendRepository;
import learntime.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StudyMemberServiceTest {

    @Autowired
    private StudyInvitationService studyInvitationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private StudyInvitationRepository studyInvitationRepository;

    /**
     * 이벤트 발행은 테스트 대상이 아니므로 Mock 처리
     */
    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    /**
     * 테스트용 User 생성
     */
    private User createUser(String name, String email) {

        return userRepository.save(
                User.builder()
                        .email(email)
                        .password("password123!")
                        .name(name)
                        .socialId(email)
                        .socialProvider(AuthProvider.LOCAL)
                        .role(Role.ROLE_USER)
                        .build()
        );
    }

    /**
     * 테스트용 Study 생성
     */
    private Study createStudy(String title) {

        return studyRepository.save(
                Study.builder()
                        .studyTitle(title)
                        .bookTitle("테스트 교재")
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(30))
                        .build()
        );
    }

    @Test
    @DisplayName("스터디 초대 성공")
    void inviteMember_success() {

        // given
        User inviter = createUser(
                "초대자",
                "inviter@test.com"
        );

        User invitedUser = createUser(
                "초대대상",
                "invited@test.com"
        );

        friendRepository.save(
                Friend.builder()
                        .user(inviter)
                        .friendUser(invitedUser)
                        .build()
        );

        Study study = createStudy("알고리즘 스터디");

        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(inviter)
                        .studyMemberRole(StudyMemberRole.OWNER)
                        .build()
        );

        StudyMemberRequestDTO request =
                new StudyMemberRequestDTO(
                        study.getStudyId(),
                        invitedUser.getUserId()
                );

        // when
        Long invitationId =
                studyInvitationService.inviteMember(
                        request,
                        inviter.getUserId()
                );

        // then
        StudyInvitation invitation =
                studyInvitationRepository.findById(invitationId)
                        .orElseThrow();

        assertThat(invitation.getStatus())
                .isEqualTo(StudyInvitationStatus.PENDING);

        assertThat(invitation.getInviterUser().getUserId())
                .isEqualTo(inviter.getUserId());

        assertThat(invitation.getInvitedUser().getUserId())
                .isEqualTo(invitedUser.getUserId());

        assertThat(invitation.getStudy().getStudyId())
                .isEqualTo(study.getStudyId());
    }

    @Test
    @DisplayName("친구 관계가 아니면 예외 발생")
    void inviteMember_fail_notFriend() {

        // given
        User inviter = createUser(
                "초대자",
                "inviter@test.com"
        );

        User invitedUser = createUser(
                "초대대상",
                "invited@test.com"
        );

        Study study = createStudy("자바 스터디");

        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(inviter)
                        .studyMemberRole(StudyMemberRole.OWNER)
                        .build()
        );

        StudyMemberRequestDTO request =
                new StudyMemberRequestDTO(
                        study.getStudyId(),
                        invitedUser.getUserId()
                );

        // when & then
        assertThatThrownBy(() ->
                studyInvitationService.inviteMember(
                        request,
                        inviter.getUserId()
                )
        ).isInstanceOf(StudyException.class)
                .hasMessage(
                        StudyErrorCode.NOT_FRIEND_RELATION.getMessage()
                );
    }

    @Test
    @DisplayName("이미 스터디 멤버이면 예외 발생")
    void inviteMember_fail_alreadyMember() {

        // given
        User inviter = createUser(
                "초대자",
                "inviter@test.com"
        );

        User invitedUser = createUser(
                "초대대상",
                "invited@test.com"
        );

        friendRepository.save(
                Friend.builder()
                        .user(inviter)
                        .friendUser(invitedUser)
                        .build()
        );

        Study study = createStudy("스프링 스터디");

        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(inviter)
                        .studyMemberRole(StudyMemberRole.OWNER)
                        .build()
        );

        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(invitedUser)
                        .studyMemberRole(StudyMemberRole.OWNER)
                        .build()
        );

        StudyMemberRequestDTO request =
                new StudyMemberRequestDTO(
                        study.getStudyId(),
                        invitedUser.getUserId()
                );

        // when & then
        assertThatThrownBy(() ->
                studyInvitationService.inviteMember(
                        request,
                        inviter.getUserId()
                )
        ).isInstanceOf(StudyException.class)
                .hasMessage(
                        StudyErrorCode.ALREADY_STUDY_MEMBER.getMessage()
                );
    }

    @Test
    @DisplayName("이미 초대 중이면 예외 발생")
    void inviteMember_fail_alreadyInvited() {

        // given
        User inviter = createUser(
                "초대자",
                "inviter@test.com"
        );

        User invitedUser = createUser(
                "초대대상",
                "invited@test.com"
        );

        friendRepository.save(
                Friend.builder()
                        .user(inviter)
                        .friendUser(invitedUser)
                        .build()
        );

        Study study = createStudy("리액트 스터디");

        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(inviter)
                        .studyMemberRole(StudyMemberRole.OWNER)
                        .build()
        );

        studyInvitationRepository.save(
                StudyInvitation.builder()
                        .study(study)
                        .invitedUser(invitedUser)
                        .inviterUser(inviter)
                        .build()
        );

        StudyMemberRequestDTO request =
                new StudyMemberRequestDTO(
                        study.getStudyId(),
                        invitedUser.getUserId()
                );

        // when & then
        assertThatThrownBy(() ->
                studyInvitationService.inviteMember(
                        request,
                        inviter.getUserId()
                )
        ).isInstanceOf(StudyException.class)
                .hasMessage(
                        StudyErrorCode.STUDY_INVITATION_ALREADY_EXISTS.getMessage()
                );
    }

    @Test
    @DisplayName("스터디 인원 제한 초과 시 예외 발생")
    void inviteMember_fail_memberLimitExceeded() {

        // given
        User inviter = createUser(
                "초대자",
                "inviter@test.com"
        );

        User invitedUser = createUser(
                "초대대상",
                "invited@test.com"
        );

        friendRepository.save(
                Friend.builder()
                        .user(inviter)
                        .friendUser(invitedUser)
                        .build()
        );

        Study study = createStudy("대규모 스터디");

        // OWNER 추가
        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(inviter)
                        .studyMemberRole(StudyMemberRole.OWNER)
                        .build()
        );

        /**
         * 현재 제한:
         * STUDY_MEMBER_LIMIT_COUNT = 4
         *
         * OWNER 1명 + MEMBER 3명
         * = 총 4명
         */
        for (int i = 0; i < 3; i++) {

            User member = createUser(
                    "멤버" + i,
                    "member" + i + "@test.com"
            );

            studyMemberRepository.save(
                    StudyMember.builder()
                            .study(study)
                            .user(member)
                            .studyMemberRole(StudyMemberRole.MEMBER)
                            .build()
            );
        }

        StudyMemberRequestDTO request =
                new StudyMemberRequestDTO(
                        study.getStudyId(),
                        invitedUser.getUserId()
                );

        // when & then
        assertThatThrownBy(() ->
                studyInvitationService.inviteMember(
                        request,
                        inviter.getUserId()
                )
        ).isInstanceOf(StudyException.class)
                .hasMessage(
                        StudyErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED.getMessage()
                );
    }

    @Test
    @DisplayName("스터디 초대 승인 성공")
    void approveRequest_success() {
        // given
        User inviter = createUser("초대자", "inviter_approve@test.com");
        User invitedUser = createUser("초대대상", "invited_approve@test.com");

        Study study = createStudy("승인 테스트 스터디");

        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(inviter)
                        .studyMemberRole(StudyMemberRole.OWNER)
                        .build()
        );

        StudyInvitation invitation = studyInvitationRepository.save(
                StudyInvitation.builder()
                        .study(study)
                        .invitedUser(invitedUser)
                        .inviterUser(inviter)
                        .build()
        );

        // when
        studyInvitationService.approveRequest(invitation.getStudyInvitationId(), invitedUser.getUserId());

        // then
        StudyInvitation updatedInvitation = studyInvitationRepository.findById(invitation.getStudyInvitationId()).orElseThrow();
        assertThat(updatedInvitation.getStatus()).isEqualTo(StudyInvitationStatus.ACCEPTED);

        boolean isMember = studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(study.getStudyId(), invitedUser.getUserId());
        assertThat(isMember).isTrue();
    }

    @Test
    @DisplayName("스터디 초대 거절 성공")
    void rejectRequest_success() {
        // given
        User inviter = createUser("초대자", "inviter_reject@test.com");
        User invitedUser = createUser("초대대상", "invited_reject@test.com");

        Study study = createStudy("거절 테스트 스터디");

        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(inviter)
                        .studyMemberRole(StudyMemberRole.OWNER)
                        .build()
        );

        StudyInvitation invitation = studyInvitationRepository.save(
                StudyInvitation.builder()
                        .study(study)
                        .invitedUser(invitedUser)
                        .inviterUser(inviter)
                        .build()
        );

        // when
        studyInvitationService.rejectRequest(invitation.getStudyInvitationId(), invitedUser.getUserId());

        // then
        StudyInvitation updatedInvitation = studyInvitationRepository.findById(invitation.getStudyInvitationId()).orElseThrow();
        assertThat(updatedInvitation.getStatus()).isEqualTo(StudyInvitationStatus.REJECTED);

        boolean isMember = studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(study.getStudyId(), invitedUser.getUserId());
        assertThat(isMember).isFalse();
    }

    @Test
    @DisplayName("스터디 초대 취소 성공")
    void cancelRequest_success() {
        // given
        User inviter = createUser("초대자", "inviter_cancel@test.com");
        User invitedUser = createUser("초대대상", "invited_cancel@test.com");

        Study study = createStudy("취소 테스트 스터디");

        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(inviter)
                        .studyMemberRole(StudyMemberRole.OWNER)
                        .build()
        );

        StudyInvitation invitation = studyInvitationRepository.save(
                StudyInvitation.builder()
                        .study(study)
                        .invitedUser(invitedUser)
                        .inviterUser(inviter)
                        .build()
        );

        // when
        studyInvitationService.cancelRequest(invitation.getStudyInvitationId(), inviter.getUserId());

        // then
        StudyInvitation updatedInvitation = studyInvitationRepository.findById(invitation.getStudyInvitationId()).orElseThrow();
        assertThat(updatedInvitation.getStatus()).isEqualTo(StudyInvitationStatus.CANCELED);
    }

    @Test
    @DisplayName("스터디 초대용 친구 목록 조회 성공")
    void getFriendsForStudyInvite_success() {
        // given
        User owner = createUser("방장", "owner@test.com");
        User friendMember = createUser("친구멤버", "member_friend@test.com");
        User friendInvited = createUser("친구초대됨", "invited_friend@test.com");
        User friendNone = createUser("친구아무것도아님", "none_friend@test.com");

        // 친구 맺기
        friendRepository.save(Friend.builder().user(owner).friendUser(friendMember).build());
        friendRepository.save(Friend.builder().user(owner).friendUser(friendInvited).build());
        friendRepository.save(Friend.builder().user(owner).friendUser(friendNone).build());

        Study study = createStudy("친구 초대 테스트 스터디");

        // 방장 등록
        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(owner)
                        .studyMemberRole(StudyMemberRole.OWNER)
                        .build()
        );

        // 친구멤버를 스터디 멤버로 등록
        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(friendMember)
                        .studyMemberRole(StudyMemberRole.MEMBER)
                        .build()
        );

        // 친구초대됨에게 초대장 발송 (PENDING)
        studyInvitationRepository.save(
                StudyInvitation.builder()
                        .study(study)
                        .invitedUser(friendInvited)
                        .inviterUser(owner)
                        .build()
        );

        // when
        List<StudyMemberFriendResponseDTO> result = studyInvitationService.getFriendsForStudyInvite(
                study.getStudyId(),
                owner.getUserId()
        );

        // then
        assertThat(result).hasSize(3);

        StudyMemberFriendResponseDTO memberFriend = result.stream()
                .filter(dto -> dto.userId().equals(friendMember.getUserId()))
                .findFirst().orElseThrow();
        assertThat(memberFriend.isInvited()).isFalse();

        StudyMemberFriendResponseDTO invitedFriend = result.stream()
                .filter(dto -> dto.userId().equals(friendInvited.getUserId()))
                .findFirst().orElseThrow();
        assertThat(invitedFriend.isInvited()).isTrue();

        StudyMemberFriendResponseDTO noneFriend = result.stream()
                .filter(dto -> dto.userId().equals(friendNone.getUserId()))
                .findFirst().orElseThrow();
        assertThat(noneFriend.isInvited()).isFalse();
    }
}