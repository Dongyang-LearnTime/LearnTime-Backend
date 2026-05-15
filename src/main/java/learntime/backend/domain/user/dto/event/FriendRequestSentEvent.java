package learntime.backend.domain.user.dto.event;

public record FriendRequestSentEvent(
        Long friendRequestId,
        Long requesterId,
        String requesterName,
        Long receiverId
) {
}
