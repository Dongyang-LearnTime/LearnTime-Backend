package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyRestDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyRestDateRepository extends JpaRepository<StudyRestDate, Long>  {
}
