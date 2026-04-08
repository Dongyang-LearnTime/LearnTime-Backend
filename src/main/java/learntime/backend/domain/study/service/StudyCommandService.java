package learntime.backend.domain.study.service;

import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.study.repository.*;
import learntime.backend.domain.study.service.component.StudyPlanDateCalculator;
import learntime.backend.domain.study.service.component.StudyRestScheduleManager;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudyCommandService {

    private final StudyRepository studyRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final UserRepository userRepository;
    private final StudyRestScheduleManager studyRestScheduleManager;
    private final PromptQuotaUtil promptQuotaUtil;
    private final StudyPlanDateCalculator studyPlanDateCalculator;

    @Transactional
    public void saveStudyPlan(GeminiStudyRequestDTO request,
                              StudyPlanResponseDTO geminiResult,
                              Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        try {
            Study study = Study.builder()
                    .studyTitle(request.studyTitle())
                    .bookTitle(request.bookTitle())
                    .startDate(request.startDate())
                    .endDate(request.endDate())
                    .user(user)
                    .build();

            studyRepository.save(study);

            // 쉬는 날짜 정보 저장
            studyRestScheduleManager.saveRestDates(study, request.restDates());
            studyRestScheduleManager.saveRestDays(study, request.restDays());

            // 쉬는 날짜를 제외한 학습 가능한 날짜 계산
            List<LocalDate> planDates = buildPlanDatesFromRequest(
                    request.startDate(),
                    geminiResult.dailyPlans().size(),
                    request.restDays(),
                    request.restDates()
            );

            List<StudyDailyPlan> dailyPlans = new java.util.ArrayList<>(geminiResult.dailyPlans().size());

            // DTO를 반복문를 돌며 엔티티로 변환
            for (int i = 0; i < geminiResult.dailyPlans().size(); i++) {
                var planDto = geminiResult.dailyPlans().get(i);

                dailyPlans.add(StudyDailyPlan.builder()
                        .study(study)
                        .dayNumber(planDto.day())
                        .planDate(planDates.get(i))
                        .planContent(planDto.tasks())
                        .build());
            }

            studyDailyPlanRepository.saveAll(dailyPlans);

        } catch (Exception e) {
            promptQuotaUtil.restorePromptQuota(userId);
            throw new BusinessException(ErrorCode.STUDY_SAVE_FAILED);
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

            // 학습 기본 정보 수정
            study.updateStudyInfo(request.studyTitle(), request.startDate(), request.endDate());

            study.getRestDates().clear();
            study.getRestDays().clear();

            studyRestScheduleManager.saveRestDates(study, request.restDates());
            studyRestScheduleManager.saveRestDays(study, request.restDays());

            // 완료되지 않은 일정만 삭제
            studyDailyPlanRepository.deleteUncompletedPlans(study, StudyDailyPlan.ProgressStatus.COMPLETED);

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

                newDailyPlans.add(StudyDailyPlan.builder()
                        .study(study)
                        .dayNumber(lastDayNumber + planDto.day())
                        .planDate(planDates.get(i))
                        .planContent(planDto.tasks())
                        .build());
            }

            studyDailyPlanRepository.saveAll(newDailyPlans);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            promptQuotaUtil.restorePromptQuota(userId);
            throw new BusinessException(ErrorCode.STUDY_SAVE_FAILED);
        }
    }

    // 남은 학습 내용을 합쳐서 문자열로
    @Transactional(readOnly = true)
    public String getRemainingStudyContent(Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        return String.join("\n", studyDailyPlanRepository.findRemainingContents(
                study,
                StudyDailyPlan.ProgressStatus.COMPLETED
        ));
    }

    @Transactional(readOnly = true)
    public int calculateRemainingStudyDays(Long studyId, GeminiReplanRequestDTO request) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        // 쉬는 날짜를 Set 으로 변환
        Set<DayOfWeek> restDays = request.restDays() == null
                ? Set.of()
                : Set.copyOf(request.restDays());

        // 쉬는 요일 Set으로 변환
        Set<LocalDate> restDates = request.restDates() == null
                ? Set.of()
                : Set.copyOf(request.restDates());

        // DB에서 완료된 학습 계획 개수 조회
        long completedCount = studyDailyPlanRepository.countCompletedPlansByStudyAndDateRange(
                study,
                StudyDailyPlan.ProgressStatus.COMPLETED,
                request.startDate(),
                request.endDate()
        );

        // 남은 학습 일수 계산
        return studyPlanDateCalculator.calculateRemainingDays(
                request.startDate(),
                request.endDate(),
                restDays,
                restDates,
                completedCount
        );
    }

    private List<LocalDate> buildPlanDatesFromRequest(
            LocalDate startDate,
            int planSize,
            List<DayOfWeek> restDays,
            List<LocalDate> restDates
    ) {
        // List → Set 변환
        Set<DayOfWeek> restDaysSet =
                restDays == null ? Set.of() : Set.copyOf(restDays);

        Set<LocalDate> restDatesSet =
                restDates == null ? Set.of() : Set.copyOf(restDates);

        // 날짜 계산 위임
        return studyPlanDateCalculator.buildPlanDates(
                startDate,
                planSize,
                restDaysSet,
                restDatesSet
        );
    }

}
