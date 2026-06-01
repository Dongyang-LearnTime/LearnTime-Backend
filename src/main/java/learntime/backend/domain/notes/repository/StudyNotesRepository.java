package learntime.backend.domain.notes.repository;

import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.study_member.model.StudyMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyNotesRepository extends JpaRepository <StudyNotes, Long> {
    List<StudyNotes> findByStudyMember(StudyMember studyMember);

    Page<StudyNotes> findByStudyMember(StudyMember studyMember, Pageable pageable);

    @Query("SELECT n FROM StudyNotes n " +
           "JOIN FETCH n.studyMember sm " +
           "JOIN FETCH sm.study s " +
           "WHERE sm.user.userId = :userId " +
           "ORDER BY n.createdAt DESC")
    List<StudyNotes> findTop3ByUserId(@Param("userId") Long userId, Pageable pageable);
}
