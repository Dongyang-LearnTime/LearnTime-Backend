package learntime.backend.domain.study.dto.response;

import learntime.backend.domain.studymember.enums.StudyPlanStatus;
import lombok.Builder;

@Builder
public record StudyStatusResponseDTO(
        Long studyId,
        StudyPlanStatus status
) {
}
