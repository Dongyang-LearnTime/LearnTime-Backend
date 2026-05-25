package learntime.backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Builder
@Schema(description = "배지 및 티어 전체 정보와 사용자의 취득 상태 응답 DTO")
public record BadgeTierInfoResponseDTO(
        @Schema(description = "전체 티어 정보 목록")
        List<TierInfoDTO> allTiers,

        @Schema(description = "전체 배지 정보 목록")
        List<BadgeInfoDTO> allBadges,

        @Schema(description = "사용자의 현재 티어 명칭", example = "자동차")
        String currentTierName,

        @Schema(description = "사용자가 획득한 배지 목록")
        List<UserBadgeResponseDTO> acquiredBadges
) {
    @Builder
    @Schema(description = "개별 티어 정보 DTO")
    public record TierInfoDTO(
            @Schema(description = "티어 명칭", example = "자동차")
            String tierName,

            @Schema(description = "요구 최소 포인트", example = "1000")
            int minPoint
    ) {}

    @Builder
    @Schema(description = "개별 배지 정보 DTO")
    public record BadgeInfoDTO(
            @Schema(description = "배지 고유 타입명", example = "EARLY_BIRD")
            String badgeType,

            @Schema(description = "배지 표시 이름", example = "일찍 일어나는 새가 벌레를")
            String displayName,

            @Schema(description = "배지 설명", example = "오전 8시 전 공부 또는 운동 중 한가지 최초 1회 완료")
            String description
    ) {}
}
