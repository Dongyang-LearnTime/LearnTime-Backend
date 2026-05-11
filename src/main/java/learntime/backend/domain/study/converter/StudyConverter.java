package learntime.backend.domain.study.converter;

import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyDailyPlanInfoResponseDTO;
import learntime.backend.domain.study.dto.response.StudyFeedbackResponseDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

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


    public static Study toStudyEntity(GeminiStudyRequestDTO request, User user) {
        return Study.builder()
                .studyTitle(request.studyTitle())
                .bookTitle(request.bookTitle())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .user(user)
                .build();
    }

    public static StudyDailyPlan toStudyDailyPlanEntity(Study study, StudyPlanResponseDTO.DailyPlan planDto, LocalDate planDate) {
        return StudyDailyPlan.builder()
                .study(study)
                .dayNumber(planDto.day())
                .planDate(planDate)
                .planContent(planDto.tasks())
                .build();
    }

    public static StudyDailyPlan toStudyDailyPlanEntity(Study study, StudyPlanResponseDTO.DailyPlan planDto, LocalDate planDate, int lastDayNumber) {
        return StudyDailyPlan.builder()
                .study(study)
                .dayNumber(lastDayNumber + planDto.day())
                .planDate(planDate)
                .planContent(planDto.tasks())
                .build();
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
