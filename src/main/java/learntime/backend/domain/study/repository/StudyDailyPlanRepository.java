package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyDailyPlanRepository extends JpaRepository<StudyDailyPlan, Long> {

    @Query("SELECT p FROM StudyDailyPlan p WHERE p.study = :study")
    List<StudyDailyPlan> findAllByStudy(@Param("study") Study study);

    // 가장 큰 일차(마지막 일차)를 조회함
    @Query("""
           SELECT COALESCE(MAX(p.dayNumber), 0)
           FROM StudyDailyPlan p
           WHERE p.study = :study
           """)
    int findMaxDayNumberByStudy(@Param("study") Study study);


    @Query("SELECT p FROM StudyDailyPlan p WHERE p.study.studyId = :studyId AND p.planDate = :planDate")
    Optional<StudyDailyPlan> findByStudyIdAndPlanDate(@Param("studyId") Long studyId, @Param("planDate") LocalDate planDate);


    // 특정 스터디의 기간 내 일일 계획들을 날짜순으로 정렬해서 조회함
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
           SELECT p.study.studyId
           FROM StudyDailyPlan p
           WHERE p.study.studyId IN :studyIds
             AND p.planDate = :planDate
           """)
    List<Long> findStudyIdsWithPlanDate(@Param("studyIds") List<Long> studyIds, @Param("planDate") LocalDate planDate);
}

