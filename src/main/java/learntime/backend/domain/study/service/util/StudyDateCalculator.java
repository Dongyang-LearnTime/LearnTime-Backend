package learntime.backend.domain.study.service.util;

import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// 학습 계획 날짜 계산 유틸리티
@Component
public class StudyDateCalculator {

    // 휴일을 제외하고 학습이 가능한 다음 날짜를 계산하여 반환합니다.
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

    // 주어진 학습 개수만큼 실제 학습이 진행될 날짜 목록을 생성합니다.
    public List<LocalDate> buildPlanDates(LocalDate startDate,
                                          int planCount,
                                          Set<DayOfWeek> restDays,
                                          Set<LocalDate> restDates) {
        List<LocalDate> planDates = new ArrayList<>(planCount);
        LocalDate current = startDate;

        for (int i = 0; i < planCount; i++) {
            current = getNextValidPlanDate(current, restDays, restDates);
            planDates.add(current);
            current = current.plusDays(1);
        }

        return planDates;
    }

    // 특정 기간 내에서 휴일과 이미 완료된 학습을 제외한 남은 학습 가능 일수를 계산합니다.
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
