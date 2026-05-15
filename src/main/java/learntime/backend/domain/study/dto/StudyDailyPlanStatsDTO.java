package learntime.backend.domain.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;

import java.time.LocalTime;

@Schema(description = "일일 학습 계획 통계 DTO")
public record StudyDailyPlanStatsDTO(
        ProgressStatus progressStatus,     // 학습 진행 상태
        CompletionStatus completionStatus, // 학습 완료 상태
        LocalTime focusTime                // 집중 시간
) {
}
