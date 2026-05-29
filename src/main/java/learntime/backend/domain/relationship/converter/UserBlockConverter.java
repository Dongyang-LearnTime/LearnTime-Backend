package learntime.backend.domain.relationship.converter;

import learntime.backend.domain.relationship.dto.response.MyBlockedUserListResponseDTO;
import learntime.backend.domain.relationship.model.UserBlock;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class UserBlockConverter {

    public UserBlockConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static UserBlock toUserBlock(User blockerUser, User blockedUser) {
        return UserBlock.builder()
                .blocker(blockerUser)
                .blocked(blockedUser)
                .build();
    }

    public static MyBlockedUserListResponseDTO toMyBlockedUserListResponseDTO(UserBlock userBlock) {
        User blockedUser = userBlock.getBlocked();

        return MyBlockedUserListResponseDTO.builder()
                .userBlockId(userBlock.getUserBlockId())
                .blockedUserId(blockedUser.getUserId())
                .blockedUserName(blockedUser.getName())
                .blockedAt(blockedUser.getCreatedAt())
                .build();
    }

}
