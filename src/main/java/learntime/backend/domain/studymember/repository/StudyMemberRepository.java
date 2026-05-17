package learntime.backend.domain.studymember.repository;

import jakarta.validation.constraints.NotNull;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.studymember.model.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    Optional<StudyMember> findByStudy_StudyIdAndUser_UserId(Long studyId, Long userId);
    List<StudyMember> findAllByStudy_StudyId(Long studyId);

    boolean existsByStudy_StudyIdAndUser_UserId(
            Long studyId,
            Long userId
    );

    long countByStudy(Study study);
}
