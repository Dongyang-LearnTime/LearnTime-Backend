package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.dto.StudyDailyPlanStatsDTO;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.model.StudyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyStatusRepository extends JpaRepository<StudyStatus, Long> {

    // 완료된 상태를 일차 순으로 조회함
    @Query("SELECT s FROM StudyStatus s JOIN FETCH s.studyDailyPlan WHERE s.studyMember.studyMemberId = :studyMemberId AND s.progressStatus = 'COMPLETED' ORDER BY s.studyDailyPlan.dayNumber ASC")
    List<StudyStatus> findCompletedStatusByStudyMemberId(@Param("studyMemberId") Long studyMemberId);

    // 멤버의 통계 지표를 위한 상태를 조회함
    @Query("SELECT new learntime.backend.domain.study.dto.StudyDailyPlanStatsDTO(s.progressStatus, s.completionStatus, s.focusTime) " +
           "FROM StudyStatus s WHERE s.studyMember.studyMemberId = :studyMemberId")
    List<StudyDailyPlanStatsDTO> findStatsByStudyMemberId(@Param("studyMemberId") Long studyMemberId);

    // 스터디 전체의 통계 지표를 위한 상태를 조회함
    @Query("SELECT new learntime.backend.domain.study.dto.StudyDailyPlanStatsDTO(s.progressStatus, s.completionStatus, s.focusTime) " +
           "FROM StudyStatus s WHERE s.studyMember.study.studyId = :studyId")
    List<StudyDailyPlanStatsDTO> findStatsById(@Param("studyId") Long studyId);

    // 기간 내 완료된 계획 개수를 계산함
    @Query("SELECT COUNT(s) FROM StudyStatus s " +
           "WHERE s.studyMember.studyMemberId = :studyMemberId " +
           "AND s.progressStatus = :completedStatus " +
           "AND s.studyDailyPlan.planDate BETWEEN :startDate AND :endDate")
    long countCompletedByMemberAndDateRange(@Param("studyMemberId") Long studyMemberId,
                                            @Param("completedStatus") ProgressStatus completedStatus,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    // 기간 내 멤버의 상태를 날짜 순으로 조회함
    @Query("SELECT s FROM StudyStatus s JOIN FETCH s.studyDailyPlan WHERE s.studyMember.studyMemberId = :studyMemberId AND s.studyDailyPlan.planDate BETWEEN :startDate AND :endDate ORDER BY s.studyDailyPlan.planDate ASC")
    List<StudyStatus> findByMemberIdAndPlanDateBetween(@Param("studyMemberId") Long studyMemberId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    // 기간 내 여러 멤버들의 상태를 날짜 순으로 조회함 (N+1 방지)
    @Query("SELECT s FROM StudyStatus s " +
           "JOIN FETCH s.studyDailyPlan " +
           "JOIN FETCH s.studyMember sm " +
           "WHERE sm.studyMemberId IN :studyMemberIds " +
           "AND s.studyDailyPlan.planDate BETWEEN :startDate AND :endDate " +
           "ORDER BY s.studyDailyPlan.planDate ASC")
    List<StudyStatus> findByStudyMemberIdInAndPlanDateBetween(@Param("studyMemberIds") List<Long> studyMemberIds,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);

    // 멤버 ID와 일일 계획 ID로 특정 상태를 찾음
    Optional<StudyStatus> findByStudyMember_StudyMemberIdAndStudyDailyPlan_StudyDailyPlanId(Long studyMemberId, Long studyDailyPlanId);

    // 미생성된 과거 상태를 실패로 일괄 생성함
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO study_status (progress_status, completion_status, study_member_id, study_daily_plan_id) " +
                   "SELECT 'COMPLETED', 'FAILURE', m.study_member_id, p.study_daily_plan_id " +
                   "FROM study_daily_plan p " +
                   "JOIN study_member m ON p.study_id = m.study_id " +
                   "WHERE p.plan_date < :targetDate " +
                   "  AND NOT EXISTS ( " +
                   "      SELECT 1 FROM study_status s " +
                   "      WHERE s.study_member_id = m.study_member_id " +
                   "        AND s.study_daily_plan_id = p.study_daily_plan_id " +
                   ")", nativeQuery = true)
    int insertMissingStatusesAsFailure(@Param("targetDate") LocalDate targetDate);

    // 기존 미완료 과거 상태를 실패로 업데이트함
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyStatus s " +
           "SET s.progressStatus = 'COMPLETED', s.completionStatus = 'FAILURE' " +
           "WHERE s.progressStatus <> 'COMPLETED' " +
           "AND s.studyDailyPlan.studyDailyPlanId IN (SELECT p.studyDailyPlanId FROM StudyDailyPlan p WHERE p.planDate < :targetDate)")
    int bulkFailIncompleteStatuses(@Param("targetDate") LocalDate targetDate);
}
