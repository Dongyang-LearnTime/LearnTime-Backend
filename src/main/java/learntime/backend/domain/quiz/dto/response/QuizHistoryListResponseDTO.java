package learntime.backend.domain.quiz.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record QuizHistoryListResponseDTO(
        List<QuizHistoryInfoDTO> histories
) {
    @Builder
    public record QuizHistoryInfoDTO(
            Long quizHistoryId,
            Integer attemptNumber,
            Integer correctCount,
            Integer totalQuestionCount,
            Integer earnedPoints,
            LocalDateTime submittedAt
    ) {}
}
