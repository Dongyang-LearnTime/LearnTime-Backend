package learntime.backend.domain.quiz.repository;

import learntime.backend.domain.quiz.model.QuizHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {
    Optional<QuizHistory> findFirstByStudyQuiz_StudyQuizIdOrderByAttemptNumberDesc(Long studyQuizId);

    @Query("SELECT qh " +
            "FROM QuizHistory qh " +
            "LEFT JOIN FETCH qh.answers " +
            "WHERE qh.studyQuiz.studyQuizId = :studyQuizId " +
            "ORDER BY qh.attemptNumber DESC")
    List<QuizHistory> findAllWithAnswersByStudyQuizId(@Param("studyQuizId") Long studyQuizId);
}
