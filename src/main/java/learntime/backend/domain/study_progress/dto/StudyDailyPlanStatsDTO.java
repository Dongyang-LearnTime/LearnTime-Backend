package learntime.backend.domain.study_progress.dto;

import learntime.backend.domain.study_progress.enums.CompletionStatus;
import learntime.backend.domain.study_progress.enums.ProgressStatus;

import java.time.LocalTime;

public record StudyDailyPlanStatsDTO(
        ProgressStatus progressStatus,
        CompletionStatus completionStatus,
        LocalTime focusTime
) {
}
