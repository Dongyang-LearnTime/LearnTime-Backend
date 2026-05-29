package learntime.backend.domain.relationship.event;

public record FriendRequestSentEvent(
        Long friendRequestId,
        Long requesterId,
        String requesterName,
        Long receiverId
) {
}
