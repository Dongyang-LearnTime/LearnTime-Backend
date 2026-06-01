package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "공부 진도 제목 수정 DTO")
public record UpdateStudyTitleRequestDTO(
        @Schema(description = "공부 ID", example = "1")
        @NotNull(message = "공부 ID는 필수입니다.")
        Long studyId,

        @Schema(description = "수정할 제목", example = "새로운 제목")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        String title
) {
}
