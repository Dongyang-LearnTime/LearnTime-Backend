package learntime.backend.domain.friend.event;

public record FriendRequestAcceptedEvent(
        Long friendRequestId,
        Long requesterId,
        Long receiverId,
        String receiverName
) {
}
