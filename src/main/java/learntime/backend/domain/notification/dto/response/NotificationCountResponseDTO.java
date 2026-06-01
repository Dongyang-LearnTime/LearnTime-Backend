package learntime.backend.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "읽지 않은 알림 수 응답 DTO")
public record NotificationCountResponseDTO(
        Long unreadCount
) {
}
