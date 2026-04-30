package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study.enums.CompletionStatus;

@Schema(description = "공부 일일 진도 완료 요청 DTO")
public record PlanCompleteRequestDTO(

        @Schema(description = "일일 공부 진도 ID")
        Long studyDailyPlanId,

        @Schema(description = "진도 성공/실패 여부 (enum)", example = "SUCCESS")
        CompletionStatus completionStatus,

        @Schema(description = "이해도 점수 1~5점")
        Integer understandingScore

) { }
