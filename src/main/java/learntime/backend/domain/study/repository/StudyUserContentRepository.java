package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyUserContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyUserContentRepository extends JpaRepository<StudyUserContent, Long> {
}
