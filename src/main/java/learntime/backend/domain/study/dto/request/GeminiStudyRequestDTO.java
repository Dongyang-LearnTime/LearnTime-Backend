package learntime.backend.domain.study.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public record GeminiStudyRequestDTO(
        @NotNull(message = "책 제목은 필수입니다.")
        String title,

        @NotNull(message = "링크는 필수입니다.")
        String linkUrl,

        @NotNull(message = "시작 날짜는 필수입니다.")
        @FutureOrPresent(message = "시작 날짜는 오늘 이후여야 합니다.")
        LocalDate startDate, // yyyy-MM-dd 형식

        @NotNull(message = "종료 날짜는 필수입니다.")
        LocalDate endDate,    // yyyy-MM-dd 형식

        List<DayOfWeek> restDays, // 쉬는 요일
        List<LocalDate> restDates // 쉬는 날짜
) {
    public int getValidatedStudyDays() {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        Set<DayOfWeek> restDaySet = (restDays == null || restDays.isEmpty())
                ? Collections.emptySet()
                : EnumSet.copyOf(restDays);

        Set<LocalDate> restDateSet = (restDates == null || restDates.isEmpty())
                ? Collections.emptySet()
                : new HashSet<>(restDates);

        // 실제 공부 일수 계산 (Stream API 활용)
        int actualStudyDays = (int) startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> !restDaySet.contains(date.getDayOfWeek())) // 요일 제거
                .filter(date -> !restDateSet.contains(date)) // 날짜 제거
                .count();

        // 쉬는 날짜, 쉬는 요일을 뺀 실제 일수에서 계산
        if (actualStudyDays < 7 || actualStudyDays > 90) {
            throw new BusinessException(ErrorCode.INVALID_STUDY_PERIOD);
        }

        return actualStudyDays;
    }
}