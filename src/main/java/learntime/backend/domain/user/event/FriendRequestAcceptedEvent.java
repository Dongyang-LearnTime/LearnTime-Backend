package learntime.backend.domain.user.event;

public record FriendRequestAcceptedEvent(
        Long friendRequestId,
        Long requesterId,
        Long receiverId,
        String receiverName
) {
}
