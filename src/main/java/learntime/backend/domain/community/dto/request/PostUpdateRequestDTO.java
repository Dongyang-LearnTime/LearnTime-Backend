package learntime.backend.domain.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import learntime.backend.domain.community.enums.PostCategory;

import java.util.List;

public record PostUpdateRequestDTO(
        @Schema(description = "수정할 게시글 제목", example = "수정된 제목")
        @NotBlank
        String title,

        @Schema(description = "수정할 게시글 내용", example = "수정된 내용")
        @NotBlank
        String content,

        @Schema(description = "삭제할 기존 이미지 URL 목록", example = "[\"https://s3.../image1.jpg\"]")
        List<String> deletedImageUrls,

        @Schema(description = "연동할 스터디 ID (선택 사항)", example = "1")
        Long studyId,

        @Schema(description = "게시글 카테고리 (FREE, RECRUITMENT)", example = "FREE")
        PostCategory category
) {
}
