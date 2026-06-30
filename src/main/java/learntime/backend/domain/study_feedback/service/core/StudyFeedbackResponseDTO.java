package learntime.backend.domain.study_feedback.service.core;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record StudyFeedbackResponseDTO(
        Long feedbackId,
        String feedbackTitle,
        String feedbackContent,
        LocalDateTime createdAt
) {
}
