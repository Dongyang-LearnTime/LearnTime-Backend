package learntime.backend.domain.study.service.core;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.study.converter.StudyDailyPlanConverter;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.service.ai.GeminiStudyService;
import learntime.backend.domain.study.service.util.StudyDateCalculator;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.enums.StudyPlanStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final PromptQuotaUtil promptQuotaUtil;
    private final StudyDateCalculator studyDateCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final GeminiStudyService geminiStudyService;

    /**
     * 비동기로 AI 계획 생성 및 상세 일정 저장
     */
    @Async
    @Transactional
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

            List<StudyDailyPlan> dailyPlans = new ArrayList<>();
            for (int i = 0; i < geminiResult.dailyPlans().size(); i++) {
                var planDto = geminiResult.dailyPlans().get(i);
                dailyPlans.add(StudyDailyPlanConverter.toStudyDailyPlanEntity(study, planDto, planDates.get(i)));
            }

            long startTime = System.currentTimeMillis();
            studyDailyPlanRepository.saveAll(dailyPlans);
            long endTime = System.currentTimeMillis();
            log.info("[StudyPlan Save] {}일 분량의 계획(복습 포함) 저장 완료. 스터디ID: {}, 소요 시간: {}ms",
                    dailyPlans.size(), study.getStudyId(), (endTime - startTime));

            study.updateStatus(StudyPlanStatus.READY);
            studyRepository.save(study);

            List<StudyMember> members = studyMemberRepository.findAllByStudy_StudyIdAndStatus(
                    study.getStudyId(),
                    StudyMemberStatus.ACTIVE
            );
            for (StudyMember member : members) {
                PointPolicy policy = member.getStudyMemberRole() == StudyMemberRole.OWNER
                        ? PointPolicy.STUDY_PLAN_CREATED
                        : PointPolicy.STUDY_PLAN_JOINED;

                eventPublisher.publishEvent(new PointEventDTO(
                        member.getUser().getUserId(),
                        policy.getAmount(),
                        PointType.EARN,
                        policy.getDescription()
                ));
            }
        } catch (Exception e) {
            log.error("비동기 학습 계획 생성 실패", e);
            promptQuotaUtil.restorePromptQuota(userId);
            if (study != null) {
                study.updateStatus(StudyPlanStatus.FAILED);
                studyRepository.save(study);
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
