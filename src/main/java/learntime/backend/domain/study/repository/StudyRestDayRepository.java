package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyRestDay;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRestDayRepository extends JpaRepository<StudyRestDay, Long>  {
}
