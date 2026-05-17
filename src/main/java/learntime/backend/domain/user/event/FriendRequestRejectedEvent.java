package learntime.backend.domain.user.event;

public record FriendRequestRejectedEvent(
        Long friendRequestId,
        Long requesterId,
        Long receiverId,
        String receiverName
) {
}
