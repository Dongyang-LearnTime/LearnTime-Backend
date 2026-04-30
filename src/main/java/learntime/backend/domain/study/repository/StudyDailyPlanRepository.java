package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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
}