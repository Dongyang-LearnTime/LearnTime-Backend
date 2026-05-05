package learntime.backend.domain.study.converter;

import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyRestDate;
import learntime.backend.domain.study.model.StudyRestDay;
import learntime.backend.domain.user.model.User;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class StudyConverter {

    public StudyConverter() { }

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
