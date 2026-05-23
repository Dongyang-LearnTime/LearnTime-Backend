package learntime.backend.domain.study_member.repository;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    Optional<StudyMember> findByStudy_StudyIdAndUser_UserId(Long studyId, Long userId);

    Optional<StudyMember> findByStudy_StudyIdAndUser_UserIdAndStatus(
            Long studyId,
            Long userId,
            StudyMemberStatus status
    );

    List<StudyMember> findAllByStudy_StudyId(Long studyId);

    List<StudyMember> findAllByStudy_StudyIdAndStatus(Long studyId, StudyMemberStatus status);

    @Query("""
        SELECT sm
        FROM StudyMember sm
        JOIN FETCH sm.user u
        WHERE sm.study.studyId = :studyId
        AND sm.status = learntime.backend.domain.study_member.enums.StudyMemberStatus.ACTIVE
    """)
    List<StudyMember> findAllActiveByStudyIdFetchUser(
            @Param("studyId") Long studyId
    );

    boolean existsByStudy_StudyIdAndUser_UserId(
            Long studyId,
            Long userId
    );

    boolean existsByStudy_StudyIdAndUser_UserIdAndStatus(
            Long studyId,
            Long userId,
            StudyMemberStatus status
    );

    long countByStudy(Study study);

    long countByStudyAndStatus(Study study, StudyMemberStatus status);

    StudyMember findByUser_UserId(Long ownerId);

    List<StudyMember> findAllByUser_UserIdAndStudyMemberRoleAndStatus(
            Long userId,
            StudyMemberRole studyMemberRole,
            StudyMemberStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StudyMember sm
            SET sm.status = learntime.backend.domain.study_member.enums.StudyMemberStatus.WITHDRAWN
            WHERE sm.user.userId = :userId
            """)
    void withdrawAllByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT sm
        FROM StudyMember sm
        JOIN FETCH sm.study s
        WHERE sm.user.userId = :userId
          AND sm.status = learntime.backend.domain.study_member.enums.StudyMemberStatus.ACTIVE
    """)
    List<StudyMember> findAllActiveByUserIdFetchStudy(@Param("userId") Long userId);
}
