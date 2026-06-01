package learntime.backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "마이페이지 요약 정보 응답 DTO")
public record MyPageSummaryResponseDTO(
        @Schema(description = "작성한 게시글 수", example = "15")
        long postCount,

        @Schema(description = "작성한 댓글 수", example = "42")
        long commentCount,

        @Schema(description = "받은 좋아요 총합", example = "108")
        long totalLikeReceived,

        @Schema(description = "보유 포인트", example = "320")
        int point
) {
}
