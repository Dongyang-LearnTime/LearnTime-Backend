package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyRestDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface StudyRestDayRepository extends JpaRepository<StudyRestDay, Long>  {
    boolean existsByStudy_StudyIdAndDayOfWeek(Long studyId, DayOfWeek dayOfWeek);
    List<StudyRestDay> findAllByStudy_StudyId(Long studyId);
}
