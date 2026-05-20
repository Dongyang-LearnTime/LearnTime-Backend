package learntime.backend.domain.notification.service;

import learntime.backend.domain.notification.converter.NotificationConverter;
import learntime.backend.domain.notification.dto.response.NotificationResponseDTO;
import learntime.backend.domain.notification.enums.NotificationReferenceType;
import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.error.code.NotificationErrorCode;
import learntime.backend.domain.notification.error.exception.NotificationException;
import learntime.backend.domain.notification.model.Notification;
import learntime.backend.domain.notification.repository.NotificationRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
// 알림을 저장하고 온라인 사용자에게 SSE로 즉시 전달하는 Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // 사용자별 여러 SSE 연결을 관리
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // SSE 연결 시간 (1시간)
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 종료/타임아웃/에러 발생 시 해당 연결만 제거
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));

        // 최초 연결 시 더미 이벤트를 보내 브라우저의 SSE 연결을 안정화
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected!"));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
            log.error("SSE 연결 중 오류 발생", e);
        }

        return emitter;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(
            Long receiverId, // 알림을 받을 사용자 ID
            NotificationType type, // 알림 종류 (친구 요청, 스터디 초대 등)
            String title, // 알림 제목
            String message, // 사용자에게 보여줄 알림 내용
            Long referenceId, // 관련 리소스 PK (친구 요청 ID, 스터디 초대 ID 등)
            NotificationReferenceType referenceType // 관련 리소스 타입
    ) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 오프라인 사용자도 나중에 확인할 수 있도록 알림을 먼저 저장합니다.
        Notification notification = notificationRepository.save(Notification.builder()
                .receiver(receiver)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build());

        NotificationResponseDTO response = NotificationConverter.toNotificationResponseDTO(notification);
        send(receiverId, response, type.getEventName());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotifications(Long userId, Long cursorId) {
        List<Notification> notifications;

        if (cursorId == null) { // 첫 페이지 조회
            notifications = notificationRepository
                    .findTop20ByReceiver_UserIdOrderByNotificationIdDesc(userId);
        } else { // 커서 기반 다음 페이지 조회
            notifications = notificationRepository
                    .findTop20ByReceiver_UserIdAndNotificationIdLessThanOrderByNotificationIdDesc(
                            userId,
                            cursorId
                    );
        }

        return notifications.stream()
                .map(NotificationConverter::toNotificationResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiver_UserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void readNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByNotificationIdAndReceiver_UserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        // 읽음 처리
        notification.markAsRead();
    }

    @Transactional
    public void readAllNotifications(Long userId) {
        notificationRepository.markAllAsReadByReceiverId(userId);
    }

    // 이미 저장된 알림 또는 저장이 필요 없는 이벤트를 SSE로 전송합니다.
    public void send(Long userId, Object data, String eventName) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException | IllegalStateException e) {
                removeEmitter(userId, emitter);
                log.warn("종료된 SSE 커넥션 감지 및 제거 완료: userId={}", userId);
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }

        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByNotificationIdAndReceiver_UserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteNotifications(Long userId, List<Long> notificationIds) {
        notificationRepository.deleteByNotificationIdInAndReceiver_UserId(notificationIds, userId);
    }


}
