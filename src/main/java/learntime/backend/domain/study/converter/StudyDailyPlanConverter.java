package learntime.backend.domain.study.converter;

import learntime.backend.domain.study.dto.response.StudyDailyPlanInfoResponseDTO;
import learntime.backend.domain.study.dto.response.StudyDailyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyStatus;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class StudyDailyPlanConverter {

    public StudyDailyPlanConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static StudyDailyPlanResponseDTO toStudyDailyPlanResponseDTO(StudyDailyPlan studyDailyPlan) {
        return StudyDailyPlanResponseDTO.builder()
                .studyDailyPlanId(studyDailyPlan.getStudyDailyPlanId())
                .dayNumber(studyDailyPlan.getDayNumber())
                .planDate(studyDailyPlan.getPlanDate())
                .planContent(studyDailyPlan.getPlanContent())
                .build();
    }

    public static StudyDailyPlanInfoResponseDTO toStudyDailyPlanInfoResponseDTO(LocalDate planDate, Study study, List<DayOfWeek> restDays, List<LocalDate> restDates, StudyDailyPlan plan, StudyStatus status, Long studyMemberId, List<Long> allStudyMemberIds) {
        if (plan == null) {
            return new StudyDailyPlanInfoResponseDTO(
                    planDate, study.getStartDate(), study.getEndDate(), restDays, restDates,
                    null, null, null, null, null, null, null, studyMemberId, allStudyMemberIds
            );
        }
        return new StudyDailyPlanInfoResponseDTO(
                planDate, study.getStartDate(), study.getEndDate(), restDays, restDates,
                plan.getStudyDailyPlanId(),
                plan.getDayNumber(),
                plan.getPlanContent(),
                status != null ? status.getFocusTime() : null,
                status != null && status.getProgressStatus() != null ? status.getProgressStatus() : ProgressStatus.NOT_STARTED,
                status != null ? status.getCompletionStatus() : null,
                status != null ? status.getUnderstandingScore() : null,
                studyMemberId,
                allStudyMemberIds
        );
    }


    public static StudyDailyPlan toStudyDailyPlanEntity(Study study, StudyPlanResponseDTO.DailyPlan planDto, LocalDate planDate) {
        return StudyDailyPlan.builder()
                .study(study)
                .dayNumber(planDto.day())
                .planDate(planDate)
                .planContent(planDto.tasks())
                .build();
    }



}
