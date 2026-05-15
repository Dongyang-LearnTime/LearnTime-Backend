package learntime.backend.domain.user.dto.event;

public record FriendRequestAcceptedEvent(
        Long friendRequestId,
        Long requesterId,
        Long receiverId,
        String receiverName
) {
}
