package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyFeedbackRepository extends JpaRepository<StudyFeedback, Long> {
    List<StudyFeedback> findAllByStudy_StudyIdOrderByCreatedAtDesc(Long studyId);
}
