package learntime.backend.domain.friend.event;

public record FriendRequestRejectedEvent(
        Long friendRequestId,
        Long requesterId,
        Long receiverId,
        String receiverName
) {
}
