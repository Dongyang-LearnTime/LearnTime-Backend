package learntime.backend.domain.study.service.component;

import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
public class StudyPlanDateCalculator {

    // 시작일 기준으로, 쉬는 요일/쉬는 날짜를 건너뛰고 실제 학습 가능한 가장 빠른 날짜를 찾음
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

    //  계획 목록을 실제 날짜에 매핑해서 반환
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

    // 남은 학습일 수를 계산한다 (총 가능한 날짜 수에서 완료된 계획 수 뺌)
    public int calculateRemainingDays(LocalDate startDate,
                                      LocalDate endDate,
                                      Set<DayOfWeek> restDays,
                                      Set<LocalDate> restDates,
                                      long completedCount) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
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
            throw new BusinessException(ErrorCode.INVALID_STUDY_PERIOD);
        }

        return remaining;
    }
}