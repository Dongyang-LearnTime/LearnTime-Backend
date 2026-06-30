package learntime.backend.domain.study_progress.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import learntime.backend.domain.study_progress.enums.CompletionStatus;

@Schema(description = "공부 일일 진도 완료 요청 DTO")
public record PlanCompleteRequestDTO(

        @Schema(description = "일일 공부 진도 ID", example = "1")
        @NotNull(message = "공부 일일 진도 ID는 필수입니다.")
        Long studyDailyPlanId,

        @Schema(description = "진도 성공/실패 여부 (enum)", example = "SUCCESS")
        @NotNull(message = "진도 상태는 필수입니다.")
        CompletionStatus completionStatus,

        @Schema(description = "이해도 점수 1~5점", example = "3")
        @NotNull(message = "understandingScore는 필수입니다.")
        @Min(value = 1, message = "이해도 점수는 최소 1점 이상이어야 합니다.")
        @Max(value = 5, message = "이해도 점수는 최대 5점 이하이어야 합니다.")
        Integer understandingScore

) { }
