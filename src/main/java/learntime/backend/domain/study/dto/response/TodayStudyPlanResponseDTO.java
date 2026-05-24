package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study.enums.ProgressStatus;
import lombok.Builder;

@Builder
@Schema(description = "오늘의 학습 계획 및 진행 상태 응답 DTO")
public record TodayStudyPlanResponseDTO(
        @Schema(description = "스터디 ID")
        Long studyId,

        @Schema(description = "스터디 제목")
        String studyTitle,

        @Schema(description = "오늘의 일일 학습 계획 ID")
        Long studyDailyPlanId,

        @Schema(description = "오늘의 학습 계획 내용")
        String planContent,

        @Schema(description = "오늘의 학습 진행 상태")
        ProgressStatus progressStatus
) {}
