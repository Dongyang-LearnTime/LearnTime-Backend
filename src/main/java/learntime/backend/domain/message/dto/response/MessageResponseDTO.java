package learntime.backend.domain.message.dto.response;

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
        Long receiverId,
        String receiverName
) {
}
