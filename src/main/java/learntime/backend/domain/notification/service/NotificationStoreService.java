package learntime.backend.domain.notification.service;

import learntime.backend.domain.notification.converter.NotificationConverter;
import learntime.backend.domain.notification.dto.response.NotificationResponseDTO;
import learntime.backend.domain.notification.enums.NotificationReferenceType;
import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.model.Notification;
import learntime.backend.domain.notification.repository.NotificationRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationStoreService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponseDTO saveNotification(
            Long receiverId,
            NotificationType type,
            String title,
            String message,
            Long referenceId,
            NotificationReferenceType referenceType
    ) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        Notification notification = notificationRepository.save(Notification.builder()
                .receiver(receiver)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build());

        return NotificationConverter.toNotificationResponseDTO(notification);
    }
}
