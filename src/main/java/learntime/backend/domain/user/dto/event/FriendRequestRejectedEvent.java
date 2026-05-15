package learntime.backend.domain.user.dto.event;

public record FriendRequestRejectedEvent(
        Long friendRequestId,
        Long requesterId,
        Long receiverId,
        String receiverName
) {
}
