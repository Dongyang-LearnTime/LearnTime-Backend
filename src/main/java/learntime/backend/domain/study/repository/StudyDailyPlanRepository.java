package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.dto.StudyDailyPlanStatsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyDailyPlanRepository extends JpaRepository<StudyDailyPlan, Long> {

    // 일일 공부 일정이 시작 전, 진행 중 이라면 실패로
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyDailyPlan s " +
            "SET s.progressStatus = 'COMPLETED', s.completionStatus = 'FAILURE' " +
            "WHERE s.progressStatus IN ('NOT_STARTED', 'IN_PROGRESS') " +
            "AND s.planDate < :targetDate") // 오늘 5시 기준이므로, '어제'까지의 계획만 타겟팅
    int bulkFailIncompletePlans(@Param("targetDate") LocalDate targetDate);

    // 가장 큰 일차(마지막 일차) 조회
    @Query("""
           SELECT COALESCE(MAX(p.dayNumber), 0)
           FROM StudyDailyPlan p
           WHERE p.study = :study
           """)
    int findMaxDayNumberByStudy(@Param("study") Study study);

    // 완료(COMPLETED) 상태가 아닌 계획들을 일괄 삭제한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           DELETE FROM StudyDailyPlan p
           WHERE p.study = :study
             AND p.progressStatus <> :completedStatus
           """)
    int deleteUncompletedPlans(@Param("study") Study study,
                               @Param("completedStatus") ProgressStatus completedStatus);

    // 특정 Study의 기간 내 완료된(COMPLETED) 학습 계획 개수를 조회한다.
    @Query("""
           SELECT COUNT(p)
           FROM StudyDailyPlan p
           WHERE p.study = :study
             AND p.progressStatus = :completedStatus
             AND p.planDate BETWEEN :startDate AND :endDate
           """)
    long countCompletedPlansByStudyAndDateRange(@Param("study") Study study,
                                                @Param("completedStatus") ProgressStatus completedStatus,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    // 특정 Study에서 완료되지 않은 학습 계획들의 내용을 조회한다.
    @Query("""
           SELECT p.planContent
           FROM StudyDailyPlan p
           WHERE p.study = :study
             AND p.progressStatus <> :completedStatus
           ORDER BY p.dayNumber ASC
           """)
    List<String> findRemainingContents(@Param("study") Study study,
                                       @Param("completedStatus") ProgressStatus completedStatus);

    @Query("SELECT p FROM StudyDailyPlan p WHERE p.study.studyId = :studyId AND p.planDate = :planDate")
    Optional<StudyDailyPlan> findByStudyIdAndPlanDate(@Param("studyId") Long studyId, @Param("planDate") LocalDate planDate);

    @Query("""
           SELECT p
           FROM StudyDailyPlan p
           WHERE p.study.studyId = :studyId
             AND p.planDate = :planDate
             AND (
                    p.studyParticipant.user.userId = :userId
                 OR (p.studyParticipant IS NULL AND p.study.user.userId = :userId)
             )
           """)
    Optional<StudyDailyPlan> findByStudyIdAndUserIdAndPlanDate(@Param("studyId") Long studyId,
                                                               @Param("userId") Long userId,
                                                               @Param("planDate") LocalDate planDate);

    @Query("""
           SELECT p
           FROM StudyDailyPlan p
           WHERE p.study.studyId = :studyId
             AND p.planDate BETWEEN :startDate AND :endDate
           ORDER BY p.planDate ASC
           """)
    List<StudyDailyPlan> findByStudyIdAndPlanDateBetweenOrderByPlanDateAsc(@Param("studyId") Long studyId,
                                                                           @Param("startDate") LocalDate startDate,
                                                                           @Param("endDate") LocalDate endDate);

    @Query("""
           SELECT p
           FROM StudyDailyPlan p
           WHERE p.study.studyId = :studyId
             AND (
                    p.studyParticipant.user.userId = :userId
                 OR (p.studyParticipant IS NULL AND p.study.user.userId = :userId)
             )
             AND p.planDate BETWEEN :startDate AND :endDate
           ORDER BY p.planDate ASC
           """)
    List<StudyDailyPlan> findByStudyIdAndUserIdAndPlanDateBetweenOrderByPlanDateAsc(@Param("studyId") Long studyId,
                                                                                    @Param("userId") Long userId,
                                                                                    @Param("startDate") LocalDate startDate,
                                                                                    @Param("endDate") LocalDate endDate);

    // 스터디의 모든 일일 일정을 일차 순으로 오름차순 조회
    List<StudyDailyPlan> findByStudyOrderByDayNumberAsc(Study study);

    @Query("SELECT p.progressStatus as progressStatus, p.completionStatus as completionStatus, p.focusTime as focusTime " +
           "FROM StudyDailyPlan p WHERE p.study.studyId = :studyId")
    List<StudyDailyPlanStatsDTO> findStatsByStudyId(@Param("studyId") Long studyId);

    @Query("""
           SELECT p.progressStatus as progressStatus, p.completionStatus as completionStatus, p.focusTime as focusTime
           FROM StudyDailyPlan p
           WHERE p.study.studyId = :studyId
             AND (
                    p.studyParticipant.user.userId = :userId
                 OR (p.studyParticipant IS NULL AND p.study.user.userId = :userId)
             )
           """)
    List<StudyDailyPlanStatsDTO> findStatsByStudyIdAndUserId(@Param("studyId") Long studyId, @Param("userId") Long userId);

    @Query("SELECT p FROM StudyDailyPlan p WHERE p.study.studyId = :studyId AND p.progressStatus = 'COMPLETED' ORDER BY p.dayNumber ASC")
    List<StudyDailyPlan> findCompletedPlansByStudyId(@Param("studyId") Long studyId);
}
