package learntime.backend.domain.quiz.repository;

import learntime.backend.domain.quiz.model.QuizHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {

    @Query(value = "SELECT qh " +
            "FROM QuizHistory qh " +
            "LEFT JOIN FETCH qh.answers " +
            "WHERE qh.studyQuiz.studyQuizId = :studyQuizId",
            countQuery = "SELECT count(qh) FROM QuizHistory qh WHERE qh.studyQuiz.studyQuizId = :studyQuizId")
    Page<QuizHistory> findAllWithAnswersByStudyQuizId(@Param("studyQuizId") Long studyQuizId, Pageable pageable);


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

    @Query("SELECT qh FROM QuizHistory qh " +
           "JOIN FETCH qh.studyQuiz sq " +
           "JOIN FETCH sq.studyMember sm " +
           "JOIN FETCH sm.study s " +
           "WHERE sm.user.userId = :userId " +
           "ORDER BY qh.submittedAt DESC")
    List<QuizHistory> findTop3ByUserId(@Param("userId") Long userId, Pageable pageable);
}
