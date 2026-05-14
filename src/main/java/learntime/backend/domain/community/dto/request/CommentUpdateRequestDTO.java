package learntime.backend.domain.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CommentUpdateRequestDTO(
        @Schema(description = "수정할 댓글 내용", example = "수정된 댓글 내용입니다.")
        @NotBlank
        String content
) {
}
