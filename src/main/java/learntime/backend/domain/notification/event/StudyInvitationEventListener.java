package learntime.backend.domain.notification.event;

import learntime.backend.domain.notification.enums.NotificationReferenceType;
import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.service.NotificationService;
import learntime.backend.domain.studymember.event.StudyInvitationAcceptedEvent;
import learntime.backend.domain.studymember.event.StudyInvitationRejectedEvent;
import learntime.backend.domain.studymember.event.StudyInvitationSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyInvitationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyInvitationSent(StudyInvitationSentEvent event) {
        try {
            String eventMessage =
                    event.inviterName() + " 님이 " + event.studyTitle() + "의 공부 진도 참여를 요청했습니다. 스터디 방 번호 : " + event.studyId();

            // 친구 요청을 받은 사용자에게 요청 도착 알림을 저장하고 전송
            notificationService.notify(
                    event.invitedUserId(),
                    NotificationType.STUDY_INVITATION_RECEIVED,
                    "공부 진도 참여 요청",
                    eventMessage,
                    event.studyInvitationId(),
                    NotificationReferenceType.STUDY_INVITATION
            );
        } catch (Exception e) {
            log.error("공부 진도 초대 실패. studyInvitationId: {}", event.studyInvitationId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyInvitationAccepted(StudyInvitationAcceptedEvent event) {
        try {
            String eventMessage =
                    event.acceptedUserName() + " 님이 " + event.studyTitle() + "의 공부 진도 참여 초대를 승인했습니다.";

            // 초대를 보낸 사용자에게 승인 알림을 저장하고 전송
            notificationService.notify(
                    event.inviterUserId(),
                    NotificationType.STUDY_INVITATION_ACCEPTED,
                    "공부 진도 초대 승인",
                    eventMessage,
                    event.studyInvitationId(),
                    NotificationReferenceType.STUDY_INVITATION
            );
        } catch (Exception e) {
            log.error("공부 진도 초대 승인 알림 생성 실패. studyInvitationId: {}", event.studyInvitationId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyInvitationRejected(StudyInvitationRejectedEvent event) {
        try {
            String eventMessage =
                    event.rejectedUserName() + " 님이 " + event.studyTitle() + "의 공부 진도 참여 초대를 거절했습니다.";

            // 초대를 보낸 사용자에게 거절 알림을 저장하고 전송
            notificationService.notify(
                    event.inviterUserId(),
                    NotificationType.STUDY_INVITATION_REJECTED,
                    "공부 진도 초대 거절",
                    eventMessage,
                    event.studyInvitationId(),
                    NotificationReferenceType.STUDY_INVITATION
            );
        } catch (Exception e) {
            log.error("공부 진도 초대 거절 알림 생성 실패. studyInvitationId: {}", event.studyInvitationId(), e);
        }
    }

}
