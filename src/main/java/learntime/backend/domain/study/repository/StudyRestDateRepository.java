package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyRestDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRestDateRepository extends JpaRepository<StudyRestDate, Long>  {
}
