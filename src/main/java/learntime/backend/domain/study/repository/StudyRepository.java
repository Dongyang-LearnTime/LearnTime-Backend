package learntime.backend.domain.study.repository;

import jakarta.persistence.LockModeType;
import learntime.backend.domain.study.model.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {

    // 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Study s
        WHERE s.studyId = :studyId
    """)
    Optional<Study> findByIdWithPessimisticLock(Long studyId);

    @Query("""
        SELECT s
        FROM Study s
        LEFT JOIN FETCH s.studyMembers sm
        LEFT JOIN FETCH sm.user
        WHERE s.studyId = :studyId
    """)
    Optional<Study> findByIdWithStudyMembersAndUser(@Param("studyId") Long studyId);

    // --- 벌크 삭제 쿼리 모음 (하위부터 삭제) ---

    // 1계층 삭제
    @Modifying
    @Query("DELETE FROM StudyMemberContent smc WHERE smc.studyMember.study.studyId = :studyId")
    void deleteStudyMemberContentsByStudyId(@Param("studyId") Long studyId);

    @Modifying
    @Query("DELETE FROM StudyStatus ss WHERE ss.studyMember.study.studyId = :studyId")
    void deleteStudyStatusesByStudyId(@Param("studyId") Long studyId);

    @Modifying
    @Query("DELETE FROM StudyFeedback sf WHERE sf.studyMember.study.studyId = :studyId")
    void deleteStudyFeedbacksByStudyId(@Param("studyId") Long studyId);

    @Modifying
    @Query("DELETE FROM QuizHistory qh WHERE qh.studyQuiz.studyMember.study.studyId = :studyId")
    void deleteQuizHistoriesByStudyId(@Param("studyId") Long studyId);

    @Modifying
    @Query("DELETE FROM QuizQuestion qq WHERE qq.studyQuiz.studyMember.study.studyId = :studyId")
    void deleteQuizQuestionsByStudyId(@Param("studyId") Long studyId);

    // 2계층 삭제
    @Modifying
    @Query("DELETE FROM StudyQuiz sq WHERE sq.studyMember.study.studyId = :studyId")
    void deleteStudyQuizzesByStudyId(@Param("studyId") Long studyId);

    // 3계층 삭제
    @Modifying
    @Query("DELETE FROM StudyDailyPlan sdp WHERE sdp.study.studyId = :studyId")
    void deleteStudyDailyPlansByStudyId(@Param("studyId") Long studyId);

    @Modifying
    @Query("DELETE FROM StudyRestDate srd WHERE srd.study.studyId = :studyId")
    void deleteStudyRestDatesByStudyId(@Param("studyId") Long studyId);

    @Modifying
    @Query("DELETE FROM StudyRestDay srdy WHERE srdy.study.studyId = :studyId")
    void deleteStudyRestDaysByStudyId(@Param("studyId") Long studyId);

    @Modifying
    @Query("DELETE FROM StudyInvitation si WHERE si.study.studyId = :studyId")
    void deleteStudyInvitationsByStudyId(@Param("studyId") Long studyId);

    // 4계층 삭제
    @Modifying
    @Query("DELETE FROM StudyMember sm WHERE sm.study.studyId = :studyId")
    void deleteStudyMembersByStudyId(@Param("studyId") Long studyId);

    // 최종 Study 삭제
    @Modifying
    @Query("DELETE FROM Study s WHERE s.studyId = :studyId")
    void deleteStudyById(@Param("studyId") Long studyId);
}
