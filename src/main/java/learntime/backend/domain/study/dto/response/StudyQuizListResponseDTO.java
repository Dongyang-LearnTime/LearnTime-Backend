package learntime.backend.domain.study.dto.response;

import learntime.backend.domain.study.enums.QuizStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record StudyQuizListResponseDTO(
        List<StudyQuizInfoDTO> quizzes
) {
    @Builder
    public record StudyQuizInfoDTO(
            Long studyQuizId,
            String quizTitle,
            QuizStatus quizStatus,
            Integer completedCount,
            LocalDateTime createdAt
    ) {}
}
