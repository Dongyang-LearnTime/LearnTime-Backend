package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.QuizHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {
    Optional<QuizHistory> findFirstByStudyQuiz_StudyQuizIdOrderByAttemptNumberDesc(Long studyQuizId);
}
