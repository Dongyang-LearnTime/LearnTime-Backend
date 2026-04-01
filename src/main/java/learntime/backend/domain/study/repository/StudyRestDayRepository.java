package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyRestDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyRestDayRepository extends JpaRepository<StudyRestDay, Long>  {
}
