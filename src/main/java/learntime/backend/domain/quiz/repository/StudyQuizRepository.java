package learntime.backend.domain.quiz.repository;

import learntime.backend.domain.quiz.model.StudyQuiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<StudyQuiz> findAllByStudyMember_StudyMemberIdOrderByCreatedAtDesc(Long studyId);

    Page<StudyQuiz> findAllByStudyMember_StudyMemberId(Long studyMemberId, Pageable pageable);
}
