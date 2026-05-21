package learntime.backend.domain.notification.event;

import learntime.backend.domain.notification.enums.NotificationReferenceType;
import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.service.NotificationService;
import learntime.backend.domain.study_member.event.StudyInvitationAcceptedEvent;
import learntime.backend.domain.study_member.event.StudyInvitationRejectedEvent;
import learntime.backend.domain.study_member.event.StudyInvitationSentEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudyInvitationEventListenerTest {

    @InjectMocks
    private StudyInvitationEventListener listener;

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("스터디 초대 시 알림 서비스가 정상적으로 호출된다")
    void handleStudyInvitationSent_Success() {
        // given
        StudyInvitationSentEvent event = new StudyInvitationSentEvent(
                1L, 100L, "알고리즘 스터디", "초대자", 200L
        );

        // when
        listener.handleStudyInvitationSent(event);

        // then
        String expectedMessage = "초대자 님이 알고리즘 스터디의 공부 진도 참여를 요청했습니다. 스터디 방 번호 : 100";
        verify(notificationService, times(1)).notify(
                eq(200L),
                eq(NotificationType.STUDY_INVITATION_RECEIVED),
                eq("공부 진도 참여 요청"),
                eq(expectedMessage),
                eq(1L),
                eq(NotificationReferenceType.STUDY_INVITATION)
        );
    }

    @Test
    @DisplayName("스터디 초대 승인 시 알림 서비스가 정상적으로 호출된다")
    void handleStudyInvitationAccepted_Success() {
        // given
        StudyInvitationAcceptedEvent event = new StudyInvitationAcceptedEvent(
                1L, 100L, "알고리즘 스터디", "초대대상", 300L
        );

        // when
        listener.handleStudyInvitationAccepted(event);

        // then
        String expectedMessage = "초대대상 님이 알고리즘 스터디의 공부 진도 참여 초대를 승인했습니다.";
        verify(notificationService, times(1)).notify(
                eq(300L),
                eq(NotificationType.STUDY_INVITATION_ACCEPTED),
                eq("공부 진도 초대 승인"),
                eq(expectedMessage),
                eq(1L),
                eq(NotificationReferenceType.STUDY_INVITATION)
        );
    }

    @Test
    @DisplayName("스터디 초대 거절 시 알림 서비스가 정상적으로 호출된다")
    void handleStudyInvitationRejected_Success() {
        // given
        StudyInvitationRejectedEvent event = new StudyInvitationRejectedEvent(
                1L, 100L, "알고리즘 스터디", "초대대상", 300L
        );

        // when
        listener.handleStudyInvitationRejected(event);

        // then
        String expectedMessage = "초대대상 님이 알고리즘 스터디의 공부 진도 참여 초대를 거절했습니다.";
        verify(notificationService, times(1)).notify(
                eq(300L),
                eq(NotificationType.STUDY_INVITATION_REJECTED),
                eq("공부 진도 초대 거절"),
                eq(expectedMessage),
                eq(1L),
                eq(NotificationReferenceType.STUDY_INVITATION)
        );
    }
}
