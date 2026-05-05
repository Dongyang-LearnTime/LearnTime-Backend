package learntime.backend.domain.study.dto.response;

import learntime.backend.domain.study.enums.QuizType;
import lombok.Builder;

import java.util.List;

@Builder
public record StudyQuizResultResponseDTO(
        Integer totalQuestionCount,
        Integer correctQuestionCount,
        Integer earnedPoints,
        List<QuizDetailResponseDTO> quizResults
) {
    @Builder
    public record QuizDetailResponseDTO(
            Long quizQuestionId,
            String questionContent,
            String userAnswer,
            String correctAnswer,
            Boolean isCorrect,
            QuizType quizType
    ) {}
}
