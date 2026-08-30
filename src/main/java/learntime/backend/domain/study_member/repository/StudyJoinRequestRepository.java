package learntime.backend.domain.study_member.repository;

import learntime.backend.domain.study_member.enums.StudyJoinRequestStatus;
import learntime.backend.domain.study_member.model.StudyJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyJoinRequestRepository extends JpaRepository<StudyJoinRequest, Long> {

    boolean existsByStudy_StudyIdAndRequesterUser_UserIdAndStatus(Long studyId, Long userId, StudyJoinRequestStatus status);

    @Query("SELECT r FROM StudyJoinRequest r " +
            "JOIN FETCH r.requesterUser " +
            "JOIN FETCH r.study " +
            "WHERE r.study.studyId = :studyId AND r.status = :status " +
            "ORDER BY r.createdAt DESC")
    List<StudyJoinRequest> findAllByStudyIdAndStatusWithDetails(
            @Param("studyId") Long studyId,
            @Param("status") StudyJoinRequestStatus status
    );

    @Query("SELECT r FROM StudyJoinRequest r " +
            "JOIN FETCH r.study " +
            "WHERE r.requesterUser.userId = :userId " +
            "ORDER BY r.createdAt DESC")
    List<StudyJoinRequest> findAllByRequesterUserIdWithStudy(@Param("userId") Long userId);

    @Query("SELECT r FROM StudyJoinRequest r " +
            "JOIN FETCH r.study " +
            "JOIN FETCH r.requesterUser " +
            "WHERE r.studyJoinRequestId = :requestId")
    Optional<StudyJoinRequest> findByIdWithDetails(@Param("requestId") Long requestId);

    @Modifying
    @Query("DELETE FROM StudyJoinRequest r WHERE r.study.studyId = :studyId")
    void deleteAllByStudyId(@Param("studyId") Long studyId);

    @Modifying
    @Query("DELETE FROM StudyJoinRequest r WHERE r.study.studyId IN :studyIds")
    void deleteAllByStudyIds(@Param("studyIds") List<Long> studyIds);
}
