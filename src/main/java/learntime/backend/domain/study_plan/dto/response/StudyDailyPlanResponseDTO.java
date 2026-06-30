package learntime.backend.domain.study_plan.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record StudyDailyPlanResponseDTO(
        Long studyDailyPlanId,
        Integer dayNumber,
        LocalDate planDate,
        String planContent
) {
}
