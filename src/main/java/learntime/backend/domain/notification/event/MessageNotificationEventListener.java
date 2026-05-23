package learntime.backend.domain.notification.event;

import learntime.backend.domain.message.event.MessageSentEvent;
import learntime.backend.domain.notification.enums.NotificationReferenceType;
import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageNotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageSent(MessageSentEvent event) {
        try {
            String eventMessage = event.senderName() + "님으로부터 쪽지가 도착했습니다.";

            // 쪽지를 받은 사용자에게 알림을 저장하고 전송
            notificationService.notify(
                    event.receiverId(),
                    NotificationType.MESSAGE_RECEIVED,
                    "쪽지 도착",
                    eventMessage,
                    event.messageId(),
                    NotificationReferenceType.MESSAGE
            );
        } catch (Exception e) {
            log.error("쪽지 수신 알림 생성 실패. messageId: {}", event.messageId(), e);
        }
    }
}
