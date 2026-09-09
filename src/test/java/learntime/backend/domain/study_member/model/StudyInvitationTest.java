package learntime.backend.domain.study_member.model;

import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.enums.StudyInvitationStatus;
import learntime.backend.domain.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class StudyInvitationTest {

    private StudyInvitation createPendingInvitation(Long inviterId, Long invitedId) {
        Study study = mock(Study.class);
        User inviter = mock(User.class);
        User invited = mock(User.class);

        given(inviter.getUserId()).willReturn(inviterId);
        given(invited.getUserId()).willReturn(invitedId);

        return StudyInvitation.builder()
                .study(study)
                .inviterUser(inviter)
                .invitedUser(invited)
                .build();
    }

    @Test
    @DisplayName("초대 생성 시 기본 상태는 PENDING이어야 한다")
    void createInvitation_DefaultStatus_IsPending() {
        StudyInvitation invitation = createPendingInvitation(1L, 2L);

        assertThat(invitation.getStatus()).isEqualTo(StudyInvitationStatus.PENDING);
        assertThat(invitation.isPending()).isTrue();
    }

    @Test
    @DisplayName("자기 자신을 초대하려고 하면 예외 발생")
    void createInvitation_SelfInvitation_ThrowsException() {
        Study study = mock(Study.class);
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);

        assertThatThrownBy(() -> StudyInvitation.builder()
                .study(study)
                .inviterUser(user)
                .invitedUser(user)
                .build())
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.SELF_INVITATION_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("PENDING 상태의 초대를 수락하면 ACCEPTED 상태로 변경된다")
    void accept_WhenPending_Success() {
        StudyInvitation invitation = createPendingInvitation(1L, 2L);

        invitation.accept();

        assertThat(invitation.getStatus()).isEqualTo(StudyInvitationStatus.ACCEPTED);
        assertThat(invitation.isPending()).isFalse();
    }

    @Test
    @DisplayName("이미 처리된(ACCEPTED) 초대를 다시 수락하려고 하면 예외 발생")
    void accept_WhenNotPending_ThrowsException() {
        StudyInvitation invitation = createPendingInvitation(1L, 2L);
        invitation.accept();

        assertThatThrownBy(invitation::accept)
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_INVITATION_NOT_PENDING.getMessage());
    }

    @Test
    @DisplayName("PENDING 상태의 초대를 거절하면 REJECTED 상태로 변경된다")
    void reject_WhenPending_Success() {
        StudyInvitation invitation = createPendingInvitation(1L, 2L);

        invitation.reject();

        assertThat(invitation.getStatus()).isEqualTo(StudyInvitationStatus.REJECTED);
        assertThat(invitation.isPending()).isFalse();
    }

    @Test
    @DisplayName("이미 거절된 초대를 다시 거절/수락하려고 하면 예외 발생")
    void reject_WhenNotPending_ThrowsException() {
        StudyInvitation invitation = createPendingInvitation(1L, 2L);
        invitation.reject();

        assertThatThrownBy(invitation::reject)
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_INVITATION_NOT_PENDING.getMessage());

        assertThatThrownBy(invitation::accept)
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_INVITATION_NOT_PENDING.getMessage());
    }

    @Test
    @DisplayName("PENDING 상태의 초대를 취소하면 CANCELED 상태로 변경된다")
    void cancel_WhenPending_Success() {
        StudyInvitation invitation = createPendingInvitation(1L, 2L);

        invitation.cancel();

        assertThat(invitation.getStatus()).isEqualTo(StudyInvitationStatus.CANCELED);
        assertThat(invitation.isPending()).isFalse();
    }

    @Test
    @DisplayName("이미 취소된 초대를 다시 취소/수락하려고 하면 예외 발생")
    void cancel_WhenNotPending_ThrowsException() {
        StudyInvitation invitation = createPendingInvitation(1L, 2L);
        invitation.cancel();

        assertThatThrownBy(invitation::cancel)
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_INVITATION_NOT_PENDING.getMessage());

        assertThatThrownBy(invitation::accept)
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_INVITATION_NOT_PENDING.getMessage());
    }
}
