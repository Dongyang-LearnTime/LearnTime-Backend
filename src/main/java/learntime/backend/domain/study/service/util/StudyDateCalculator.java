package learntime.backend.domain.study.service.util;

import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

// 학습 계획 날짜 계산 유틸리티
@Component
public class StudyDateCalculator {

    public LocalDate getNextValidPlanDate(LocalDate startDate,
                                          Set<DayOfWeek> restDays,
                                          Set<LocalDate> restDates) {
        LocalDate date = startDate;

        while (true) {
            boolean isRestDay = restDays != null && restDays.contains(date.getDayOfWeek());
            boolean isRestDate = restDates != null && restDates.contains(date);

            if (!isRestDay && !isRestDate) {
                return date;
            }

            date = date.plusDays(1);
        }
    }

    public List<LocalDate> buildPlanDates(LocalDate startDate,
                                          int planCount,
                                          Set<DayOfWeek> restDays,
                                          Set<LocalDate> restDates) {
        List<LocalDate> planDates = new java.util.ArrayList<>(planCount);
        LocalDate current = startDate;

        for (int i = 0; i < planCount; i++) {
            current = getNextValidPlanDate(current, restDays, restDates);
            planDates.add(current);
            current = current.plusDays(1);
        }

        return planDates;
    }

    public int calculateRemainingDays(LocalDate startDate,
                                      LocalDate endDate,
                                      Set<DayOfWeek> restDays,
                                      Set<LocalDate> restDates,
                                      long completedCount) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new StudyException(StudyErrorCode.INVALID_DATE_RANGE);
        }

        int totalDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            boolean isRestDay = restDays != null && restDays.contains(current.getDayOfWeek());
            boolean isRestDate = restDates != null && restDates.contains(current);

            if (!isRestDay && !isRestDate) {
                totalDays++;
            }

            current = current.plusDays(1);
        }

        int remaining = totalDays - (int) completedCount;

        if (remaining <= 0) {
            throw new StudyException(StudyErrorCode.INVALID_STUDY_PERIOD);
        }

        return remaining;
    }
}
