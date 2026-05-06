package learntime.backend.domain.quiz.dto.response;

import learntime.backend.domain.quiz.enums.QuizType;

public record QuizQuestionResponseDTO(
        String questionContent,
        String correctAnswer,
        QuizType quizType
) {
}
