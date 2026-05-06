package learntime.backend.domain.quiz.repository;

import learntime.backend.domain.quiz.model.QuizQuestion;
import learntime.backend.domain.quiz.model.StudyQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository <QuizQuestion, Long> {
    List<QuizQuestion> findAllByStudyQuiz(StudyQuiz studyQuiz);
}
