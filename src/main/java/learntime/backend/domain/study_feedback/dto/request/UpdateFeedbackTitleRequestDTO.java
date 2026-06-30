package learntime.backend.domain.study_feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "AI 피드백 제목 수정 요청 DTO")
public record UpdateFeedbackTitleRequestDTO(
        @Schema(description = "피드백 ID", example = "1")
        @NotNull(message = "피드백 ID는 필수입니다.")
        Long feedbackId,

        @Schema(description = "수정할 제목", example = "새로운 제목")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 150, message = "제목은 150자를 초과할 수 없습니다.")
        String feedbackTitle
) {
}
