package learntime.backend.domain.study_plan.converter;

import learntime.backend.domain.study_plan.dto.response.StudyDailyPlanInfoResponseDTO;
import learntime.backend.domain.study_plan.dto.response.StudyDailyPlanResponseDTO;
import learntime.backend.domain.study_plan.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study_progress.enums.CompletionStatus;
import learntime.backend.domain.study_progress.enums.ProgressStatus;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study_progress.model.StudyStatus;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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

    public static StudyDailyPlanInfoResponseDTO toStudyDailyPlanInfoResponseDTO(
            LocalDate planDate,
            Study study,
            List<DayOfWeek> restDays,
            List<LocalDate> restDates,
            StudyDailyPlan plan,
            StudyStatus status,
            Long studyMemberId,
            List<Long> allStudyMemberIds
    ) {
        Long studyDailyPlanId = plan != null ? plan.getStudyDailyPlanId() : null;
        Integer dayNumber = plan != null ? plan.getDayNumber() : null;
        String planContent = plan != null ? plan.getPlanContent() : null;

        LocalTime focusTime = status != null ? status.getFocusTime() : null;
        CompletionStatus completionStatus = status != null ? status.getCompletionStatus() : null;
        Integer understandingScore = status != null ? status.getUnderstandingScore() : null;

        ProgressStatus progressStatus = ProgressStatus.NOT_STARTED;
        if (status != null && status.getProgressStatus() != null) {
            progressStatus = status.getProgressStatus();
        }

        return StudyDailyPlanInfoResponseDTO.builder()
                .studyTitle(study.getStudyTitle())
                .bookTitle(study.getBookTitle())
                .planDate(planDate)
                .startDate(study.getStartDate())
                .endDate(study.getEndDate())
                .restDays(restDays)
                .restDates(restDates)

                // 계획 정보
                .studyDailyPlanId(studyDailyPlanId)
                .dayNumber(dayNumber)
                .planContent(planContent)

                // 진행 상태 정보
                .focusTime(focusTime)
                .progressStatus(progressStatus)
                .completionStatus(completionStatus)
                .understandingScore(understandingScore)

                // 멤버 정보
                .studyMemberId(studyMemberId)
                .allStudyMemberIds(allStudyMemberIds)

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



}
