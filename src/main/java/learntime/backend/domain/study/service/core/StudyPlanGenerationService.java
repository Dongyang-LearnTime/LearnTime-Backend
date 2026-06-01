package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.service.ai.GeminiStudyService;
import learntime.backend.domain.study.service.util.StudyDateCalculator;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyPlanGenerationService {

    private final StudyRepository studyRepository;
    private final PromptQuotaUtil promptQuotaUtil;
    private final StudyDateCalculator studyDateCalculator;
    private final GeminiStudyService geminiStudyService;
    private final StudyPlanStoreService studyPlanStoreService;

    /**
     * 비동기로 AI 계획 생성 및 상세 일정 저장
     */
    @Async
    public void generateAndSavePlanAsync(Long studyId, GeminiStudyRequestDTO request, Long userId) {
        Study study = null;
        try {
            study = studyRepository.findById(studyId)
                    .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

            StudyPlanResponseDTO geminiResult = geminiStudyService.generateSmartStudyPlan(request, userId);
            List<LocalDate> planDates = buildPlanDatesFromRequest(
                    request.startDate(),
                    geminiResult.dailyPlans().size(),
                    request.getRestDaysAsDayOfWeek(),
                    request.restDates()
            );

            studyPlanStoreService.saveGeneratedPlanAndEvents(study.getStudyId(), geminiResult, planDates);

        } catch (Exception e) {
            log.error("비동기 학습 계획 생성 실패", e);
            promptQuotaUtil.restorePromptQuota(userId);
            if (study != null) {
                studyPlanStoreService.updateStudyStatusFailed(study.getStudyId());
            }
        }
    }

    private List<LocalDate> buildPlanDatesFromRequest(
            LocalDate startDate,
            int planSize,
            List<DayOfWeek> restDays,
            List<LocalDate> restDates
    ) {
        Set<DayOfWeek> restDaysSet = restDays == null ? Set.of() : Set.copyOf(restDays);
        Set<LocalDate> restDatesSet = restDates == null ? Set.of() : Set.copyOf(restDates);

        return studyDateCalculator.buildPlanDates(
                startDate,
                planSize,
                restDaysSet,
                restDatesSet
        );
    }
}
