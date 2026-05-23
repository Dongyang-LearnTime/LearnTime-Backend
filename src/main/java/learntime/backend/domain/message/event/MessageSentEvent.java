package learntime.backend.domain.message.event;

public record MessageSentEvent(
        Long messageId,
        Long senderId,
        String senderName,
        Long receiverId
) {
}
