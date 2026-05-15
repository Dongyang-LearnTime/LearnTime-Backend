package learntime.backend.domain.study.converter;

import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyDailyPlanInfoResponseDTO;
import learntime.backend.domain.study.dto.response.StudyFeedbackResponseDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.StudyRecentWeekInfoResponseDTO;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StudyConverter {

    public StudyConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static StudyDailyPlanInfoResponseDTO toStudyDailyPlanInfoResponseDTO(LocalDate planDate, Study study, List<DayOfWeek> restDays, List<LocalDate> restDates, StudyDailyPlan plan) {
        if (plan == null) {
            return new StudyDailyPlanInfoResponseDTO(
                    planDate, study.getStartDate(), study.getEndDate(), restDays, restDates,
                    null, null, null, null, null, null, null
            );
        }
        return new StudyDailyPlanInfoResponseDTO(
                planDate, study.getStartDate(), study.getEndDate(), restDays, restDates,
                plan.getStudyDailyPlanId(),
                plan.getDayNumber(),
                plan.getPlanContent(),
                plan.getFocusTime(),
                plan.getProgressStatus(),
                plan.getCompletionStatus(),
                plan.getUnderstandingScore()
        );
    }

    public static StudyFeedbackResponseDTO toStudyFeedbackResponseDTO(StudyFeedback feedback) {
        return StudyFeedbackResponseDTO.builder()
                .feedbackId(feedback.getStudyFeedbackId())
                .feedbackTitle(feedback.getFeedbackTitle())
                .feedbackContent(feedback.getFeedbackContent())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    public static List<StudyRecentWeekInfoResponseDTO> toRecentWeekStudyInfoResponseDTOs(
            List<StudyDailyPlan> plans,
            LocalDate today,
            Set<DayOfWeek> restDays,
            Set<LocalDate> restDates
    ) {
        LocalDate startDate = today.minusDays(7);

        Map<LocalDate, StudyDailyPlan> planByDate = plans.stream()
                .filter(plan -> plan.getPlanDate() != null)
                .filter(plan -> !plan.getPlanDate().isBefore(startDate) && plan.getPlanDate().isBefore(today))
                .collect(Collectors.toMap(
                        StudyDailyPlan::getPlanDate,
                        plan -> plan,
                        (first, second) -> first
                ));

        return startDate.datesUntil(today)
                .filter(date -> !restDays.contains(date.getDayOfWeek()))
                .filter(date -> !restDates.contains(date))
                .map(date -> toStudyRecentWeekInfoResponseDTO(date, planByDate.get(date)))
                .toList();
    }

    private static StudyRecentWeekInfoResponseDTO toStudyRecentWeekInfoResponseDTO(LocalDate planDate, StudyDailyPlan plan) {
        if (plan == null) {
            return new StudyRecentWeekInfoResponseDTO(planDate, null, null, null, null);
        }

        return new StudyRecentWeekInfoResponseDTO(
                planDate,
                plan.getFocusTime(),
                plan.getProgressStatus(),
                plan.getCompletionStatus(),
                plan.getUnderstandingScore()
        );
    }


    public static Study toStudyEntity(GeminiStudyRequestDTO request, User user) {
        return Study.builder()
                .studyTitle(request.studyTitle())
                .bookTitle(request.bookTitle())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .user(user)
                .build();
    }

    public static StudyDailyPlan toStudyDailyPlanEntity(Study study, StudyParticipant participant, StudyPlanResponseDTO.DailyPlan planDto, LocalDate planDate) {
        return StudyDailyPlan.builder()
                .study(study)
                .studyParticipant(participant)
                .dayNumber(planDto.day())
                .planDate(planDate)
                .planContent(planDto.tasks())
                .build();
    }

    public static StudyDailyPlan toStudyDailyPlanEntity(Study study, StudyPlanResponseDTO.DailyPlan planDto, LocalDate planDate) {
        return toStudyDailyPlanEntity(study, null, planDto, planDate);
    }

    public static StudyDailyPlan toStudyDailyPlanEntity(Study study, StudyParticipant participant, StudyPlanResponseDTO.DailyPlan planDto, LocalDate planDate, int lastDayNumber) {
        return StudyDailyPlan.builder()
                .study(study)
                .studyParticipant(participant)
                .dayNumber(lastDayNumber + planDto.day())
                .planDate(planDate)
                .planContent(planDto.tasks())
                .build();
    }

    public static StudyDailyPlan toStudyDailyPlanEntity(Study study, StudyPlanResponseDTO.DailyPlan planDto, LocalDate planDate, int lastDayNumber) {
        return toStudyDailyPlanEntity(study, null, planDto, planDate, lastDayNumber);
    }

    public static StudyRestDate toStudyRestDateEntity(Study study, LocalDate date) {
        return StudyRestDate.builder()
                .study(study)
                .restDate(date)
                .build();
    }

    public static StudyRestDay toStudyRestDayEntity(Study study, DayOfWeek dayOfWeek) {
        return StudyRestDay.builder()
                .study(study)
                .dayOfWeek(dayOfWeek)
                .build();
    }
}
