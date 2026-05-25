package learntime.backend.domain.profile.dto.response;

import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import learntime.backend.domain.profile.enums.ProfileVisibility;
import learntime.backend.domain.user.dto.response.UserBadgeResponseDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record ProfileResponseDTO(
        Long userId,
        String name,
        Integer point,
        String tierName,
        String profileImageUrl,
        String description,
        ProfileVisibility profileVisibility,
        Long friendCount,
        List<UserBadgeResponseDTO> badges,
        List<PostListResponseDTO> recentPosts
) {}
