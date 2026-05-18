package learntime.backend.domain.studymember.repository;

import jakarta.validation.constraints.NotNull;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.studymember.model.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    Optional<StudyMember> findByStudy_StudyIdAndUser_UserId(Long studyId, Long userId);

    List<StudyMember> findAllByStudy_StudyId(Long studyId);

    @Query("""
        SELECT sm
        FROM StudyMember sm
        JOIN FETCH sm.user u
        WHERE sm.study.studyId = :studyId
    """)
    List<StudyMember> findAllByStudyIdFetchUser(
            @Param("studyId") Long studyId
    );

    boolean existsByStudy_StudyIdAndUser_UserId(
            Long studyId,
            Long userId
    );

    long countByStudy(Study study);

    StudyMember findByUser_UserId(Long ownerId);
}
