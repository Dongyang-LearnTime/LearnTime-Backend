package learntime.backend.domain.message.dto.response;

import learntime.backend.domain.user.enums.Role;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record MessageResponseDTO(
        Long messageId,
        String content,
        LocalDateTime sentAt,
        LocalDateTime readAt,
        Long senderId,
        String senderName,
        Role senderRole,
        Long receiverId,
        String receiverName
) {
}
