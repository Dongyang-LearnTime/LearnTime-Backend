package learntime.backend.domain.notes.repository;

import learntime.backend.domain.notes.model.StudyNotes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyNotesRepository extends JpaRepository <StudyNotes, Long> {
    List<StudyNotes> findByStudy_StudyId(Long studyId);
}
