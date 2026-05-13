package learntime.backend.domain.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequestDTO(
        @Schema(description = "게시글 제목", example = "오늘 공부 인증합니다!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
        String title,

        @Schema(description = "게시글 내용", example = "열심히 공부했어요.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 1000, message = "내용은 1000자를 넘을 수 없습니다.")
        String content,

        @Schema(description = "관련 스터디 ID (선택 사항)", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long studyId
) {
}
