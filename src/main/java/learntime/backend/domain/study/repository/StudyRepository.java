package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.Study;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRepository extends JpaRepository<Study, Long> {
}
