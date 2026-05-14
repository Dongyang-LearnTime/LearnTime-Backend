package learntime.backend.domain.study.dto;

import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;

import java.time.LocalTime;

public record StudyDailyPlanStatsDTO(
        ProgressStatus progressStatus,
        CompletionStatus completionStatus,
        LocalTime focusTime
) {
}
