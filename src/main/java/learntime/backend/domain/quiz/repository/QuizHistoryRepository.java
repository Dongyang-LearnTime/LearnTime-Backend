package learntime.backend.domain.quiz.repository;

import learntime.backend.domain.quiz.model.QuizHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {

    @Query("SELECT qh " +
            "FROM QuizHistory qh " +
            "LEFT JOIN FETCH qh.answers " +
            "WHERE qh.studyQuiz.studyQuizId = :studyQuizId " +
            "ORDER BY qh.attemptNumber DESC")
    List<QuizHistory> findAllWithAnswersByStudyQuizId(@Param("studyQuizId") Long studyQuizId);


    @Query("SELECT COUNT(qa.quizAnswerId), SUM(CASE WHEN qa.isCorrect = true THEN 1 ELSE 0 END) " +
            "FROM QuizHistory qh " +
            "JOIN qh.answers qa " +
            "WHERE qh.studyQuiz.studyMember.studyMemberId = :studyMemberId")
    List<Object[]> findQuizStatsByStudyMemberId(@Param("studyMemberId") Long studyMemberId);

    @Query("SELECT COUNT(qa.quizAnswerId), SUM(CASE WHEN qa.isCorrect = true THEN 1 ELSE 0 END) " +
            "FROM QuizHistory qh " +
            "JOIN qh.answers qa " +
            "WHERE qh.studyQuiz.studyMember.study.studyId = :studyId")
    List<Object[]> findQuizStatsByStudyId(@Param("studyId") Long studyId);

}
