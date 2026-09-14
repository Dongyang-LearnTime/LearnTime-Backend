package learntime.backend.domain.study_member.repository;

import jakarta.persistence.LockModeType;
import learntime.backend.domain.study_member.enums.StudyInvitationStatus;
import learntime.backend.domain.study_member.model.StudyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface StudyInvitationRepository extends JpaRepository<StudyInvitation, Long> {

    // 특정 상태의 공부 진도 초대 요청이 있는지 확인
    boolean existsByStudy_StudyIdAndInvitedUser_UserIdAndStatus(
            Long studyId,
            Long invitedUserId,
            StudyInvitationStatus status
    );

    // 받은 초대 목록
    @Query("""
        SELECT si
        FROM StudyInvitation si
        JOIN FETCH si.study s
        JOIN FETCH si.inviterUser iu
        WHERE si.invitedUser.userId = :userId
        AND si.status = :status
    """)
    List<StudyInvitation> findAllByInvitedUser_UserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") StudyInvitationStatus status
    );


    // 보낸 초대 목록
    @Query("""
        SELECT si
        FROM StudyInvitation si
        JOIN FETCH si.study s
        JOIN FETCH si.invitedUser iu
        WHERE si.inviterUser.userId = :userId
        AND si.status = :status
    """)
    List<StudyInvitation> findAllByInviterUser_UserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") StudyInvitationStatus status
    );

    @Modifying
    @Query("DELETE FROM StudyInvitation s WHERE s.status = :status AND s.updatedAt <= :cutoffDate")
    void deleteOldInvitationsByStatus(
            @Param("status") StudyInvitationStatus status,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StudyInvitation s
            SET s.status = StudyInvitationStatus.CANCELED
                , s.updatedAt = :updatedAt
            WHERE s.status = StudyInvitationStatus.PENDING
              AND (s.invitedUser.userId = :userId OR s.inviterUser.userId = :userId)
            """)
    void cancelPendingByUserId(
            @Param("userId") Long userId,
            @Param("updatedAt") LocalDateTime updatedAt
    );
    @Query("""
        SELECT si
        FROM StudyInvitation si
        JOIN FETCH si.invitedUser iu
        WHERE si.study.studyId = :studyId
        AND si.status = :status
    """)
    List<StudyInvitation> findAllByStudy_StudyIdAndStatusFetchInvitedUser(
            @Param("studyId") Long studyId,
            @Param("status") StudyInvitationStatus status
    );

    /** validateInvitation에서 study·invitedUser·inviterUser를 모두 접근하므로 한 번에 Fetch Join */
    @Query("""
        SELECT si
        FROM StudyInvitation si
        JOIN FETCH si.study s
        JOIN FETCH si.invitedUser iu
        JOIN FETCH si.inviterUser iur
        WHERE si.studyInvitationId = :invitationId
    """)
    Optional<StudyInvitation> findByIdFetchAll(@Param("invitationId") Long invitationId);

    // 비관적 락: 초대 수락 시 동시성 제어를 위해 초대 행 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT si
        FROM StudyInvitation si
        WHERE si.studyInvitationId = :invitationId
    """)
    Optional<StudyInvitation> findByIdWithPessimisticLock(@Param("invitationId") Long invitationId);

}
