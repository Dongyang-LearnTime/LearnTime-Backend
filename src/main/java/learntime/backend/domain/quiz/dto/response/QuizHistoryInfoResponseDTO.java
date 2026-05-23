package learntime.backend.domain.quiz.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record QuizHistoryInfoResponseDTO(
        Long quizHistoryId,
        Integer attemptNumber,
        Integer correctCount,
        Integer totalQuestionCount,
        Integer earnedPoints,
        LocalDateTime submittedAt
) { }
