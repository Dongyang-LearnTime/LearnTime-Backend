package learntime.backend.domain.profile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import learntime.backend.domain.profile.enums.ProfileVisibility;
import learntime.backend.domain.user.dto.response.UserBadgeResponseDTO;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "프로필 조회 응답 DTO")
public record ProfileResponseDTO(
        @Schema(description = "사용자 식별자")
        Long userId,

        @Schema(description = "사용자 이름")
        String name,

        @Schema(description = "활동 포인트")
        Integer point,

        @Schema(description = "티어 이름")
        String tierName,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "상태 메시지 및 설명")
        String description,

        @Schema(description = "프로필 공개 범위")
        ProfileVisibility profileVisibility,

        @Schema(description = "친구 수")
        Long friendCount,

        @Schema(description = "현재 로그인한 사용자와 친구 여부")
        Boolean isFriend,

        @Schema(description = "현재 로그인한 사용자가 보낸 대기 중인 친구 요청 여부")
        Boolean hasPendingSentRequest,

        @Schema(description = "현재 로그인한 사용자에게 온 대기 중인 친구 요청 여부")
        Boolean hasPendingReceivedRequest,

        @Schema(description = "대기 중인 친구 요청 식별자 (수락/거절/취소 처리용)")
        Long pendingFriendRequestId,

        @Schema(description = "보유한 뱃지 목록")
        List<UserBadgeResponseDTO> badges,

        @Schema(description = "최근 작성한 게시물 목록 (최대 5개)")
        List<PostListResponseDTO> recentPosts
) {}
