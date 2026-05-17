package learntime.backend.domain.notification.event;

import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.service.NotificationService;
import learntime.backend.domain.studymember.event.StudyInvitationSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyInvitationEventListener {

    private final NotificationService notificationService;

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
                    "STUDY_INVITATION"
            );
        } catch (Exception e) {
            log.error("공부 진도 초대 실패. studyInvitationId: {}", event.studyInvitationId(), e);
        }
    }
}
