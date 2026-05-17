package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    Optional<StudyMember> findByStudy_StudyIdAndUser_UserId(Long studyId, Long userId);
    List<StudyMember> findAllByStudy_StudyId(Long studyId);
}
