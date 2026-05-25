package learntime.backend.domain.friend.event;

public record FriendRequestSentEvent(
        Long friendRequestId,
        Long requesterId,
        String requesterName,
        Long receiverId
) {
}
