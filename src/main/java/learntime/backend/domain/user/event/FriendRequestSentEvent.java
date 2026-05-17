package learntime.backend.domain.user.event;

public record FriendRequestSentEvent(
        Long friendRequestId,
        Long requesterId,
        String requesterName,
        Long receiverId
) {
}
