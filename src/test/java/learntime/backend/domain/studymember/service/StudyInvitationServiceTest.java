package learntime.backend.domain.studymember.service;

import learntime.backend.domain.study.enums.StudyRole;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.studymember.dto.request.StudyMemberRequestDTO;
import learntime.backend.domain.studymember.enums.StudyInvitationStatus;
import learntime.backend.domain.studymember.model.StudyInvitation;
import learntime.backend.domain.studymember.model.StudyMember;
import learntime.backend.domain.studymember.repository.StudyInvitationRepository;
import learntime.backend.domain.studymember.repository.StudyMemberRepository;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.model.Friend;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.FriendRepository;
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
                        .studyRole(StudyRole.Owner)
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
                        .studyRole(StudyRole.Owner)
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
                        .studyRole(StudyRole.Owner)
                        .build()
        );

        studyMemberRepository.save(
                StudyMember.builder()
                        .study(study)
                        .user(invitedUser)
                        .studyRole(StudyRole.Member)
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
                        .studyRole(StudyRole.Owner)
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
                        .studyRole(StudyRole.Owner)
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
                            .studyRole(StudyRole.Member)
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
}