package learntime.backend.domain.profile.converter;

import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import learntime.backend.domain.profile.dto.response.ProfileResponseDTO;
import learntime.backend.domain.profile.model.Profile;
import learntime.backend.domain.user.dto.response.UserBadgeResponseDTO;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

import java.util.List;

public class ProfileConverter {

    public ProfileConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static ProfileResponseDTO toProfileResponseDTO(
            User user,
            Profile profile,
            String tierName,
            Long friendCount,
            Boolean isFriend,
            Boolean hasPendingSentRequest,
            Boolean hasPendingReceivedRequest,
            Long pendingFriendRequestId,
            List<UserBadgeResponseDTO> badges,
            List<PostListResponseDTO> recentPosts
    ) {
        return ProfileResponseDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .point(user.getPoint())
                .tierName(tierName)
                .profileImageUrl(profile.getProfileImageUrl())
                .description(profile.getDescription())
                .profileVisibility(profile.getProfileVisibility())
                .friendCount(friendCount)
                .isFriend(isFriend)
                .hasPendingSentRequest(hasPendingSentRequest)
                .hasPendingReceivedRequest(hasPendingReceivedRequest)
                .pendingFriendRequestId(pendingFriendRequestId)
                .badges(badges)
                .recentPosts(recentPosts)
                .build();
    }
}
