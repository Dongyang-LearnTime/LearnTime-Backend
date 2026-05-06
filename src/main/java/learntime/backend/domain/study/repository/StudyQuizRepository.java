package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyQuizRepository extends JpaRepository<StudyQuiz, Long> {
    @Query("SELECT sq FROM StudyQuiz sq JOIN FETCH sq.questions WHERE sq.studyQuizId = :studyQuizId")
    Optional<StudyQuiz> findByIdWithQuestions(@Param("studyQuizId") Long studyQuizId);

    List<StudyQuiz> findAllByStudy_StudyIdOrderByCreatedAtDesc(Long studyId);
}
