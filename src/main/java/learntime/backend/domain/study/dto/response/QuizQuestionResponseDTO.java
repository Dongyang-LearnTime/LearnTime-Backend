package learntime.backend.domain.study.dto.response;

import learntime.backend.domain.study.enums.QuizType;

public record QuizQuestionResponseDTO(
        String questionContent,
        String correctAnswer,
        QuizType quizType
) {
}