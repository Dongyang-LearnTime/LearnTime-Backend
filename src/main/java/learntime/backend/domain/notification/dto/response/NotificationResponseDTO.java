package learntime.backend.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.notification.enums.NotificationReferenceType;
import learntime.backend.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

@Schema(description = "알림 정보 응답 DTO")
public record NotificationResponseDTO(
        Long notificationId,       // 알림 식별자
        NotificationType type,     // 알림 타입
        String title,              // 알림 제목
        String message,            // 알림 메시지 내용
        Long referenceId,          // 관련 도메인 식별자 (선택)
        NotificationReferenceType referenceType, // 관련 도메인 타입 (선택)
        Boolean isRead,            // 읽음 여부
        LocalDateTime createdAt    // 생성 일시
) {
}
