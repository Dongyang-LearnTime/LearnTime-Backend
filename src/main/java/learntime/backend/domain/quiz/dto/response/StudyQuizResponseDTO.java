package learntime.backend.domain.quiz.dto.response;

import learntime.backend.domain.quiz.enums.QuizStatus;
import learntime.backend.domain.quiz.enums.QuizType;
import lombok.Builder;

import java.util.List;

public record StudyQuizResponseDTO(
        String quizTitle,
        QuizStatus quizStatus,
        List<QuizQuestionInfoDTO> questions
) {
    @Builder
    public record QuizQuestionInfoDTO(
            Long quizQuestionId,
            String questionContent,
            QuizType quizType
    ) {}
}
