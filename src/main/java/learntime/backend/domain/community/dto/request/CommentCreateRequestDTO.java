package learntime.backend.domain.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentCreateRequestDTO(
        @Schema(description = "댓글을 달 게시글 ID", example = "1")
        @NotNull
        Long postId,
        @Schema(description = "댓글 내용", example = "댓글 내용입니다.")
        @NotBlank
        String content
) {
}
