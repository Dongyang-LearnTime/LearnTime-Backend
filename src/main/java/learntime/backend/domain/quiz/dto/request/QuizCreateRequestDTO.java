package learntime.backend.domain.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "퀴즈 생성 요청 DTO")
public record QuizCreateRequestDTO (
        @Schema(description = "공부 ID", example = "1")
        @NotNull(message = "공부 ID는 필수입니다.")
        Long studyId,

        @Schema(description = "공부 필기 ID", example = "1")
        @NotNull(message = "공부 필기 ID는 필수입니다.")
        Long studyNotesId
) {}
