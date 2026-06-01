package learntime.backend.domain.relationship.converter;

import learntime.backend.domain.relationship.dto.response.FriendRequestResponseDTO;
import learntime.backend.domain.relationship.dto.response.FriendResponseDTO;
import learntime.backend.domain.relationship.model.Friend;
import learntime.backend.domain.relationship.model.FriendRequest;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class FriendConverter {

    public FriendConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static FriendResponseDTO toFriendResponseDTO(Friend friend, Long currentUserId) {
        User friendUser = friend.getUser().getUserId().equals(currentUserId)
                ? friend.getFriendUser()
                : friend.getUser();

        return new FriendResponseDTO(
                friend.getFriendId(),
                friendUser.getUserId(),
                friendUser.getName(),
                friendUser.getEmail(),
                friend.getCreatedAt()
        );
    }

    public static FriendRequestResponseDTO toFriendRequestResponseDTO(FriendRequest request) {
        return new FriendRequestResponseDTO(
                request.getFriendRequestId(),
                request.getRequester().getUserId(),
                request.getRequester().getName(),
                request.getReceiver().getUserId(),
                request.getReceiver().getName(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
