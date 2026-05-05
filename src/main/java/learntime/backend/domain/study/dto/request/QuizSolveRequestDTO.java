package learntime.backend.domain.study.dto.request;

import lombok.Builder;

@Builder
public record QuizSolveRequestDTO(
        Long quizQuestionId,
        String userAnswer
) { }
