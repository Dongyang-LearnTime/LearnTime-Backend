package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyDailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyDailyPlanRepository extends JpaRepository<StudyDailyPlan, Long>  {
}
