package learntime.backend.domain.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import learntime.backend.domain.community.enums.PostCategory;

public record PostCreateRequestDTO(
        @Schema(description = "게시글 제목", example = "오늘 공부 인증합니다!")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
        String title,

        @Schema(description = "게시글 내용", example = "열심히 공부했어요.")
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 1000, message = "내용은 1000자를 넘을 수 없습니다.")
        String content,

        @Schema(description = "관련 스터디 ID (모집글인 경우 필수)", example = "1")
        Long studyId,

        @Schema(description = "게시글 카테고리 (FREE: 일반, RECRUITMENT: 스터디원 모집)", example = "FREE")
        PostCategory category,

        @Schema(description = "공지사항 여부", example = "false")
        boolean isNotice
) {
    public PostCreateRequestDTO {
        if (category == null) {
            category = PostCategory.FREE;
        }
    }
}
