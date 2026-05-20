package learntime.backend.domain.notification.event;

import learntime.backend.domain.notification.enums.NotificationReferenceType;
import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.service.NotificationService;
import learntime.backend.domain.user.event.FriendRequestAcceptedEvent;
import learntime.backend.domain.user.event.FriendRequestRejectedEvent;
import learntime.backend.domain.user.event.FriendRequestSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendNotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequestSent(FriendRequestSentEvent event) {
        try {
            // 친구 요청을 받은 사용자에게 요청 도착 알림을 저장하고 전송
            notificationService.notify(
                    event.receiverId(),
                    NotificationType.FRIEND_REQUEST_RECEIVED,
                    "친구 요청",
                    event.requesterName() + "님이 친구 요청을 보냈습니다.",
                    event.friendRequestId(),
                    NotificationReferenceType.FRIEND_REQUEST
            );
        } catch (Exception e) {
            log.error("친구 요청 알림 생성 실패. friendRequestId: {}", event.friendRequestId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        try {
            // 친구 요청을 보낸 사용자에게 승인 알림을 저장하고 전송
            notificationService.notify(
                    event.requesterId(),
                    NotificationType.FRIEND_REQUEST_ACCEPTED,
                    "친구 요청 승인",
                    event.receiverName() + "님이 친구 요청을 승인했습니다.",
                    event.friendRequestId(),
                    NotificationReferenceType.FRIEND_REQUEST
            );
        } catch (Exception e) {
            log.error("친구 요청 승인 알림 생성 실패. friendRequestId: {}", event.friendRequestId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequestRejected(FriendRequestRejectedEvent event) {
        try {
            // 친구 요청을 보낸 사용자에게 거절 알림을 저장하고 전송
            notificationService.notify(
                    event.requesterId(),
                    NotificationType.FRIEND_REQUEST_REJECTED,
                    "친구 요청 거절",
                    event.receiverName() + "님이 친구 요청을 거절했습니다.",
                    event.friendRequestId(),
                    NotificationReferenceType.FRIEND_REQUEST
            );
        } catch (Exception e) {
            log.error("친구 요청 거절 알림 생성 실패. friendRequestId: {}", event.friendRequestId(), e);
        }
    }
}
