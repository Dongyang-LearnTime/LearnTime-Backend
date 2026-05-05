package learntime.backend.domain.study.service.core;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.study.repository.*;
import learntime.backend.domain.study.service.util.StudyDateCalculator;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

// 학습 도메인의 핵심 비즈니스 로직 및 DB 연산을 담당하는 서비스
@Service
@RequiredArgsConstructor
public class StudyCoreService {

    private final StudyRepository studyRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final UserRepository userRepository;
    private final StudyRestManager studyRestManager;
    private final PromptQuotaUtil promptQuotaUtil;
    private final StudyDateCalculator studyDateCalculator;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void saveStudyPlan(GeminiStudyRequestDTO request,
                              StudyPlanResponseDTO geminiResult,
                              Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        try {
            Study study = StudyConverter.toStudyEntity(request, user);

            studyRepository.save(study);

            // 쉬는 날짜 정보 저장
            studyRestManager.saveRestDates(study, request.restDates());
            studyRestManager.saveRestDays(study, request.restDays());

            // 쉬는 날짜를 제외한 학습 가능한 날짜 계산
            List<LocalDate> planDates = buildPlanDatesFromRequest(
                    request.startDate(),
                    geminiResult.dailyPlans().size(),
                    request.restDays(),
                    request.restDates()
            );

            List<StudyDailyPlan> dailyPlans = new java.util.ArrayList<>(geminiResult.dailyPlans().size());

            for (int i = 0; i < geminiResult.dailyPlans().size(); i++) {
                var planDto = geminiResult.dailyPlans().get(i);
                dailyPlans.add(StudyConverter.toStudyDailyPlanEntity(study, planDto, planDates.get(i)));
            }

            studyDailyPlanRepository.saveAll(dailyPlans);

            eventPublisher.publishEvent(
                    new PointEventDTO(userId,
                            PointPolicy.STUDY_PLAN_CREATED.getAmount(),
                            PointType.EARN,
                            PointPolicy.STUDY_PLAN_CREATED.getDescription()
                    )
            );

        } catch (Exception e) {
            promptQuotaUtil.restorePromptQuota(userId);
            throw new StudyException(StudyErrorCode.STUDY_SAVE_FAILED);
        }
    }

    @Transactional
    public void replanStudy(Long studyId,
                            GeminiReplanRequestDTO request,
                            StudyPlanResponseDTO geminiResult,
                            Long userId) {
        try {
            Study study = studyRepository.findById(studyId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

            study.updateStudyInfo(request.studyTitle(), request.startDate(), request.endDate());

            study.getRestDates().clear();
            study.getRestDays().clear();

            studyRestManager.saveRestDates(study, request.restDates());
            studyRestManager.saveRestDays(study, request.restDays());

            studyDailyPlanRepository.deleteUncompletedPlans(study, ProgressStatus.COMPLETED);

            int lastDayNumber = studyDailyPlanRepository.findMaxDayNumberByStudy(study);

            List<LocalDate> planDates = buildPlanDatesFromRequest(
                    request.startDate(),
                    geminiResult.dailyPlans().size(),
                    request.restDays(),
                    request.restDates()
            );

            List<StudyDailyPlan> newDailyPlans = new java.util.ArrayList<>(geminiResult.dailyPlans().size());

            for (int i = 0; i < geminiResult.dailyPlans().size(); i++) {
                var planDto = geminiResult.dailyPlans().get(i);
                newDailyPlans.add(StudyConverter.toStudyDailyPlanEntity(study, planDto, planDates.get(i), lastDayNumber));
            }

            studyDailyPlanRepository.saveAll(newDailyPlans);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            promptQuotaUtil.restorePromptQuota(userId);
            throw new StudyException(StudyErrorCode.STUDY_SAVE_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public String getRemainingStudyContent(Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        return String.join("\n", studyDailyPlanRepository.findRemainingContents(
                study,
                ProgressStatus.COMPLETED
        ));
    }

    // 쉬는 날, 쉬는 요일 추출
    @Transactional(readOnly = true)
    public int calculateRemainingStudyDays(Long studyId, GeminiReplanRequestDTO request) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        Set<DayOfWeek> restDays = request.restDays() == null
                ? Set.of()
                : Set.copyOf(request.restDays());

        Set<LocalDate> restDates = request.restDates() == null
                ? Set.of()
                : Set.copyOf(request.restDates());

        long completedCount = studyDailyPlanRepository.countCompletedPlansByStudyAndDateRange(
                study,
                ProgressStatus.COMPLETED,
                request.startDate(),
                request.endDate()
        );

        return studyDateCalculator.calculateRemainingDays(
                request.startDate(),
                request.endDate(),
                restDays,
                restDates,
                completedCount
        );
    }

    @Transactional
    public void deleteStudy(Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("공부 진도를 찾을 수 없습니다."));

        studyRepository.deleteById(studyId);
    }

    private List<LocalDate> buildPlanDatesFromRequest(
            LocalDate startDate,
            int planSize,
            List<DayOfWeek> restDays,
            List<LocalDate> restDates
    ) {
        Set<DayOfWeek> restDaysSet =
                restDays == null ? Set.of() : Set.copyOf(restDays);

        Set<LocalDate> restDatesSet =
                restDates == null ? Set.of() : Set.copyOf(restDates);

        return studyDateCalculator.buildPlanDates(
                startDate,
                planSize,
                restDaysSet,
                restDatesSet
        );
    }

}
