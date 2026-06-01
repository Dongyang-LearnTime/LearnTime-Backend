package learntime.backend.domain.quiz.dto.response;

import learntime.backend.domain.quiz.enums.QuizStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record StudyQuizInfoResponseDTO(
        Long studyQuizId,
        String quizTitle,
        QuizStatus quizStatus,
        Integer completedCount,
        LocalDateTime createdAt
) { }
