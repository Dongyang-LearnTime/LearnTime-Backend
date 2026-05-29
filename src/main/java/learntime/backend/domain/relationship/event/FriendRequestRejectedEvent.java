package learntime.backend.domain.relationship.event;

public record FriendRequestRejectedEvent(
        Long friendRequestId,
        Long requesterId,
        Long receiverId,
        String receiverName
) {
}
