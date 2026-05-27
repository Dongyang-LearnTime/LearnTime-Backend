package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyRestDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudyRestDateRepository extends JpaRepository<StudyRestDate, Long>  {
    // 특정 스터디의 지정 휴무일을 모두 조회함
    List<StudyRestDate> findAllByStudy_StudyId(Long studyId);

    void deleteAllByStudy_StudyId(Long studyId);
}
