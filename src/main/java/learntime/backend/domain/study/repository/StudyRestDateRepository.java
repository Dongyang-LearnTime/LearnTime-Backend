package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyRestDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudyRestDateRepository extends JpaRepository<StudyRestDate, Long>  {
    boolean existsByStudy_StudyIdAndRestDate(Long studyId, LocalDate restDate);
    List<StudyRestDate> findAllByStudy_StudyId(Long studyId);
}
