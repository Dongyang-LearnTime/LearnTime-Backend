package learntime.backend.domain.study.converter;

import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.*;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study_member.enums.StudyPlanStatus;
import learntime.backend.domain.study_member.model.StudyMember;
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


    public static Study toStudyEntity(GeminiStudyRequestDTO request, User user) {
        return Study.builder()
                .studyTitle(request.studyTitle())
                .bookTitle(request.bookTitle())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(StudyPlanStatus.PLANNING)
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

    public static StudyFeedbackResponseDTO toStudyFeedbackResponseDTO(StudyFeedback feedback) {
        return StudyFeedbackResponseDTO.builder()
                .feedbackId(feedback.getStudyFeedbackId())
                .feedbackTitle(feedback.getFeedbackTitle())
                .feedbackContent(feedback.getFeedbackContent())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    public static StudyMemberRecentWeekInfoResponseDTO toStudyMemberRecentWeekInfoResponseDTO(
            StudyMember member,
            List<StudyDailyPlan> plans,
            List<StudyStatus> statuses,
            LocalDate today,
            Set<DayOfWeek> restDays,
            Set<LocalDate> restDates
    ) {
        List<StudyRecentWeekInfoResponseDTO> memberRecentWeekInfos = toRecentWeekStudyInfoResponseDTOs(
                plans, statuses, today, restDays, restDates
        );
        return new StudyMemberRecentWeekInfoResponseDTO(
                member.getStudyMemberId(),
                member.getUser().getName(),
                memberRecentWeekInfos
        );
    }

    public static List<StudyRecentWeekInfoResponseDTO> toRecentWeekStudyInfoResponseDTOs(
            List<StudyDailyPlan> plans,
            List<StudyStatus> statuses,
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

        Map<LocalDate, StudyStatus> statusByDate = statuses.stream()
                .filter(status -> status.getStudyDailyPlan() != null && status.getStudyDailyPlan().getPlanDate() != null)
                .collect(Collectors.toMap(
                        status -> status.getStudyDailyPlan().getPlanDate(),
                        status -> status,
                        (first, second) -> first
                ));

        return startDate.datesUntil(today)
                .map(date -> {
                    boolean isRestDay = restDays.contains(date.getDayOfWeek()) || restDates.contains(date);
                    return toStudyRecentWeekInfoResponseDTO(date, planByDate.get(date), statusByDate.get(date), isRestDay);
                })
                .toList();
    }

    private static StudyRecentWeekInfoResponseDTO toStudyRecentWeekInfoResponseDTO(LocalDate planDate, StudyDailyPlan plan, StudyStatus status, boolean isRestDay) {
        if (plan == null) {
            return new StudyRecentWeekInfoResponseDTO(planDate, null, null, null, null, isRestDay);
        }

        return new StudyRecentWeekInfoResponseDTO(
                planDate,
                status != null ? status.getFocusTime() : null,
                status != null && status.getProgressStatus() != null ? status.getProgressStatus() : ProgressStatus.NOT_STARTED,
                status != null ? status.getCompletionStatus() : null,
                status != null ? status.getUnderstandingScore() : null,
                isRestDay
        );
    }

    public static StudyMemberContentResponseDTO toStudyMemberContentResponseDTO(StudyMemberContent content) {
        return new StudyMemberContentResponseDTO(
                content.getStudyMemberContentId(),
                content.getStudyDailyPlan().getStudyDailyPlanId(),
                content.getStudyDailyPlan().getDayNumber(),
                content.getMemberContent()
        );
    }

    public static StudyProgressIndicatorResponseDTO toStudyProgressIndicatorResponseDTO(Long studyId, String studyTitle, boolean hasTodayPlan) {
        return StudyProgressIndicatorResponseDTO.builder()
                .studyId(studyId)
                .studyTitle(studyTitle)
                .hasTodayPlan(hasTodayPlan)
                .build();
    }

}
