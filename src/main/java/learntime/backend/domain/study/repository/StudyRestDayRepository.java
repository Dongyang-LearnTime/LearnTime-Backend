package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyRestDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface StudyRestDayRepository extends JpaRepository<StudyRestDay, Long>  {
    // 특정 스터디의 정기 휴무 요일을 모두 조회함
    List<StudyRestDay> findAllByStudy_StudyId(Long studyId);
}
