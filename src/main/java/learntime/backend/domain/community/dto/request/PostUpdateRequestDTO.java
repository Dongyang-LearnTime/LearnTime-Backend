package learntime.backend.domain.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record PostUpdateRequestDTO(
        @Schema(description = "수정할 게시글 제목", example = "수정된 제목")
        @NotBlank
        String title,

        @Schema(description = "수정할 게시글 내용", example = "수정된 내용")
        @NotBlank
        String content,

        @Schema(description = "삭제할 기존 이미지 URL 목록", example = "[\"https://s3.../image1.jpg\"]")
        List<String> deletedImageUrls
) {
}
