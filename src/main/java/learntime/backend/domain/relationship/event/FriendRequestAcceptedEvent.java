package learntime.backend.domain.relationship.event;

public record FriendRequestAcceptedEvent(
        Long friendRequestId,
        Long requesterId,
        Long receiverId,
        String receiverName
) {
}
