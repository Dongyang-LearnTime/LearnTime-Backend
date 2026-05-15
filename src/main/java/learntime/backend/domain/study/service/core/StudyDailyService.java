package learntime.backend.domain.study.service.core;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.study.dto.request.PlanCompleteRequestDTO;
import learntime.backend.domain.study.dto.response.StudyDailyPlanInfoResponseDTO;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.repository.StudyRestDateRepository;
import learntime.backend.domain.study.repository.StudyRestDayRepository;
import learntime.backend.domain.study.service.util.StudyParticipantAccessVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import learntime.backend.domain.study.model.StudyRestDay;
import learntime.backend.domain.study.model.StudyRestDate;
import learntime.backend.domain.study.converter.StudyConverter;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

// 일일 진도 및 포인트 지급 관련 비즈니스 로직 담당 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyDailyService {

    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyRestDateRepository studyRestDateRepository;
    private final StudyRestDayRepository studyRestDayRepository;
    private final StudyRepository studyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;
    private final StudyShareService studyShareService;

    private static final int UNDERSTANDING_SCORE_WEIGHT = 2; // 이해도에 따른 가중치 (이해도 2면 10*2)

    // 특정 날짜의 학습 계획 정보를 조회합니다.
    @Transactional(readOnly = true)
    public StudyDailyPlanInfoResponseDTO getStudyPlanInfoByDate(Long studyId, LocalDate planDate, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_DAILY_NOT_FOUND));

        studyShareService.getActiveParticipant(studyId, userId);

        List<DayOfWeek> restDays = studyRestDayRepository.findAllByStudy_StudyId(studyId)
                .stream().map(StudyRestDay::getDayOfWeek).toList();

        List<LocalDate> restDates = studyRestDateRepository.findAllByStudy_StudyId(studyId)
                .stream().map(StudyRestDate::getRestDate).toList();

        StudyDailyPlan studyDailyPlan = studyDailyPlanRepository.findByStudyIdAndUserIdAndPlanDate(studyId, userId, planDate)
                .orElse(null);

        return StudyConverter.toStudyDailyPlanInfoResponseDTO(planDate, study, restDays, restDates, studyDailyPlan);
    }

    // 일일 학습 계획을 완료 처리하고 포인트를 지급합니다.
    @Transactional
    public int completeStudyDailyPlan(PlanCompleteRequestDTO request, Long userId) {
        StudyDailyPlan studyDailyPlan = studyDailyPlanRepository.findById(request.studyDailyPlanId())
                .orElseThrow(() -> new IllegalArgumentException("공부 일일 진도를 찾을 수 없습니다."));

        StudyParticipantAccessVerifier.verifyDailyPlanParticipant(studyDailyPlan, userId);

        studyDailyPlan.setProgressStatus(ProgressStatus.COMPLETED);
        studyDailyPlan.setCompletionStatus(request.completionStatus());
        studyDailyPlan.setUnderstandingScore(request.understandingScore());
        studyDailyPlan.setCompletionDate(LocalDateTime.now());

        int calculatedPoint = calculatePoint(request.completionStatus(), request.understandingScore());
        String description = determineDescription(request.completionStatus(), request.understandingScore());

        eventPublisher.publishEvent(new PointEventDTO(
                userId,
                calculatedPoint,
                PointType.EARN,
                description
        ));

        // 통계 지표 캐시 무효화 (진도 상태가 변경되었으므로)
        Cache cache = cacheManager.getCache("studyTotalIndicator");
        if (cache != null) {
            cache.evict(studyDailyPlan.getStudy().getStudyId() + ":" + userId);
        }
        Cache recentWeekCache = cacheManager.getCache("studyRecentWeekInfo");
        if (recentWeekCache != null) {
            recentWeekCache.evict(studyDailyPlan.getStudy().getStudyId() + ":" + userId);
        }

        return calculatedPoint;
    }

    // 학습 완료 상태와 이해도에 따라 지급할 포인트를 계산합니다.
    private int calculatePoint(CompletionStatus status, int understandingScore) {
        if (status == CompletionStatus.SUCCESS) {
            int bonus = understandingScore * UNDERSTANDING_SCORE_WEIGHT;
            return PointPolicy.STUDY_COMPLETED_SUCCESS.getAmount() + bonus;
        }
        return PointPolicy.STUDY_COMPLETED_FAILURE.getAmount();
    }

    // 포인트 지급 내역에 표시될 설명을 결정합니다.
    private String determineDescription(CompletionStatus status, int understandingScore) {
        if (status == CompletionStatus.SUCCESS) {
            return String.format("%s (이해도: %d점)",
                    PointPolicy.STUDY_COMPLETED_SUCCESS.getDescription(), understandingScore);
        }
        return PointPolicy.STUDY_COMPLETED_FAILURE.getDescription();
    }

    // 완료되지 않는 진도 실패 처리
    @Transactional
    public void markIncompletePlansAsFailure() {
        LocalDate today = LocalDate.now();

        log.info("[StudyDailyPlan] 미완료 계획 실패 처리 시작 - 기준일: {}", today);
        long startTime = System.currentTimeMillis();

        int updatedCount =
                studyDailyPlanRepository.bulkFailIncompletePlans(today);

        log.info("[StudyDailyPlan] 실패 처리 대상 건수: {}", updatedCount);

        if (updatedCount > 0) {
            Cache cache = cacheManager.getCache("studyTotalIndicator");
            if (cache != null) {
                cache.clear();
                log.info("[StudyDailyPlan] studyTotalIndicator 캐시 초기화 완료");
            } else {
                log.warn("[StudyDailyPlan] studyTotalIndicator 캐시를 찾을 수 없음");
            }
            Cache recentWeekCache = cacheManager.getCache("studyRecentWeekInfo");
            if (recentWeekCache != null) {
                recentWeekCache.clear();
                log.info("[StudyDailyPlan] studyRecentWeekInfo 캐시 초기화 완료");
            } else {
                log.warn("[StudyDailyPlan] studyRecentWeekInfo 캐시를 찾을 수 없음");
            }
        }
        long endTime = System.currentTimeMillis();
        log.info(
                "[StudyDailyPlan] 실패 처리 완료 - 처리 건수: {}건, 소요 시간: {}ms",
                updatedCount,
                (endTime - startTime)
        );

    }

}
