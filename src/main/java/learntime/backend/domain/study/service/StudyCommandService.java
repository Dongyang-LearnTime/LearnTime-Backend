package learntime.backend.domain.study.service;

import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.study.repository.*;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyCommandService {

    private final StudyRepository studyRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyRestDateRepository studyRestDateRepository;
    private final StudyRestDayRepository studyRestDayRepository;

    @Transactional
    public void saveStudyPlan(GeminiStudyRequestDTO request, StudyPlanResponseDTO geminiResult) {
        Study study = Study.builder()
                .studyTitle(request.studyTitle())
                .bookTitle(request.bookTitle())
                .startDate(request.startDate())
                .endDate(request.endDate())
                // userId 컬럼이 Study 엔티티에 추가되면 여기에 매핑
                .build();

        studyRepository.save(study);

        List<StudyDailyPlan> dailyPlans = geminiResult.dailyPlans().stream()
                .map(planDto -> StudyDailyPlan.builder()
                        .study(study) // 외래키
                        .dayNumber(planDto.day())
                        .planContent(planDto.tasks())
                        .build())
                .toList();

        studyDailyPlanRepository.saveAll(dailyPlans);

        // 쉬는 날이 존재한다면 저장
        if (!CollectionUtils.isEmpty(request.restDates())) {
            List<StudyRestDate> restDates = request.restDates().stream()
                    .map(date -> StudyRestDate.builder()
                            .study(study)
                            .restDate(date)
                            .build())
                    .toList();

            studyRestDateRepository.saveAll(restDates);
        }

        // 쉬는 요일이 존재한다면 저장
        if (!CollectionUtils.isEmpty(request.restDays())) {
            List<StudyRestDay> restDays = request.restDays().stream()
                    .map(dayOfWeek -> StudyRestDay.builder()
                            .study(study)
                            .dayOfWeek(dayOfWeek)
                            .build())
                    .toList();

            studyRestDayRepository.saveAll(restDays);
        }
    }

    @Transactional
    public void replanStudy(Long studyId, GeminiReplanRequestDTO request, StudyPlanResponseDTO geminiResult) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        // 학습 기간 및 제목 등 기본 정보 업데이트
        study.updateStudyInfo(request.studyTitle(), request.startDate(), request.endDate());

        //  기존 쉬는 날 / 쉬는 요일 초기화 (orphanRemoval=true 에 의해 DB에서 삭제됨)
        study.getRestDates().clear();
        study.getRestDays().clear();

        if (!CollectionUtils.isEmpty(request.restDates())) {
            List<StudyRestDate> restDates = request.restDates().stream()
                    .map(date -> StudyRestDate.builder().study(study).restDate(date).build())
                    .toList();
            studyRestDateRepository.saveAll(restDates);
        }

        if (!CollectionUtils.isEmpty(request.restDays())) {
            List<StudyRestDay> restDays = request.restDays().stream()
                    .map(dayOfWeek -> StudyRestDay.builder().study(study).dayOfWeek(dayOfWeek).build())
                    .toList();
            studyRestDayRepository.saveAll(restDays);
        }

        // 기존 진도 중 진행 완료(COMPLETED) 상태가 "아닌" 진도만 삭제
        study.getStudyDailyPlans().removeIf(plan ->
                plan.getProgressStatus() != StudyDailyPlan.ProgressStatus.COMPLETED);

        // 완료된 일정 이후부터 일차(Day)가 이어지도록 가장 마지막 일차 계산
        // 예: 5일차까지 완료되었다면, 새 진도는 6일차부터 매핑되게 함
        int lastDayNumber = study.getStudyDailyPlans().stream()
                .mapToInt(StudyDailyPlan::getDayNumber)
                .max()
                .orElse(0);

        // 새 진도 추가
        List<StudyDailyPlan> newDailyPlans = geminiResult.dailyPlans().stream()
                .map(planDto -> StudyDailyPlan.builder()
                        .study(study)
                        .dayNumber(lastDayNumber + planDto.day())
                        .planContent(planDto.tasks())
                        .build())
                .toList();

        studyDailyPlanRepository.saveAll(newDailyPlans);
    }

    // 남은 미완료 학습 내용 추출 메서드
    @Transactional(readOnly = true)
    public String getRemainingStudyContent(Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        // 완료되지 않은(COMPLETED가 아닌) 진도들의 내용을 하나로 합쳐서 반환
        return study.getStudyDailyPlans().stream()
                .filter(plan -> plan.getProgressStatus() != StudyDailyPlan.ProgressStatus.COMPLETED)
                .map(StudyDailyPlan::getPlanContent)
                .collect(Collectors.joining("\n"));
    }

    // 새로 설정된 기간과 휴일(쉬는 요일, 쉬는 날)을 반영하고, 이미 완료된 일수를 빼서 '실제 남은 학습 일수' 계산
    @Transactional(readOnly = true)
    public int calculateRemainingStudyDays(Long studyId, GeminiReplanRequestDTO request) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        int totalDays = 0;
        LocalDate currentDate = request.startDate();
        LocalDate endDate = request.endDate();

        if (currentDate == null || endDate == null || currentDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        while (!currentDate.isAfter(endDate)) {
            boolean isRestDay = !CollectionUtils.isEmpty(request.restDays()) && request.restDays().contains(currentDate.getDayOfWeek());
            boolean isRestDate = !CollectionUtils.isEmpty(request.restDates()) && request.restDates().contains(currentDate);

            if (!isRestDay && !isRestDate) {
                totalDays++;
            }
            currentDate = currentDate.plusDays(1);
        }

        // 이미 완료된(COMPLETED) 진도의 개수 차감
        long completedDaysCount = study.getStudyDailyPlans().stream()
                .filter(plan -> plan.getProgressStatus() == StudyDailyPlan.ProgressStatus.COMPLETED)
                .count();

        int remainingDays = totalDays - (int) completedDaysCount;

        if (remainingDays <= 0) {
            throw new BusinessException(ErrorCode.INVALID_STUDY_PERIOD);
        }
        return remainingDays;
    }
}
