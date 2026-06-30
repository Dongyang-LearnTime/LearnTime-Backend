package learntime.backend.domain.study_progress.dto.response;

import lombok.Builder;

@Builder
public record StudyProgressIndicatorResponseDTO(
        Long studyId,
        String studyTitle,
        boolean hasTodayPlan
) {
}
