package learntime.backend.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사이트 현황 통계 응답 DTO")
public record SiteStatsResponseDTO(
        @Schema(description = "전체 가입자 수") long totalUsers,
        @Schema(description = "오늘 가입한 가입자 수") long todayNewUsers,
        @Schema(description = "전체 게시글 수") long totalPosts,
        @Schema(description = "오늘 작성된 게시글 수") long todayNewPosts,
        @Schema(description = "전체 댓글 수") long totalComments,
        @Schema(description = "오늘 작성된 댓글 수") long todayNewComments
) {}
