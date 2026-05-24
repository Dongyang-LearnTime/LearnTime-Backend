package learntime.backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Builder
@Schema(description = "사용자 요약 정보(포인트, 티어, 배지) 응답 DTO")
public record UserSummaryResponseDTO(
        @Schema(description = "사용자 보유 포인트")
        Integer point,

        @Schema(description = "사용자 티어 명칭")
        String tierName,

        @Schema(description = "획득한 배지 목록")
        List<UserBadgeResponseDTO> badges,

        @Schema(description = "다음 티어 도달에 필요한 포인트")
        int nextMinPoint
) {}
