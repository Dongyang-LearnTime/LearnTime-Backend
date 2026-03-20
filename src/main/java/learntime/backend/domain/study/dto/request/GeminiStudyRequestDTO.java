package learntime.backend.domain.study.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record GeminiStudyRequestDTO(
        @NotNull(message = "책 제목은 필수입니다.")
        String title,

        @NotNull(message = "링크는 필수입니다.")
        String linkUrl,

        @NotNull(message = "시작 날짜는 필수입니다.")
        @FutureOrPresent(message = "시작 날짜는 오늘 이후여야 합니다.")
        LocalDate startDate, // yyyy-MM-dd 형식

        @NotNull(message = "종료 날짜는 필수입니다.")
        LocalDate endDate    // yyyy-MM-dd 형식
) {
    public int getValidatedStudyDays() {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;

        if (days < 7 || days > 90) {
            throw new BusinessException(ErrorCode.INVALID_STUDY_PERIOD);
        }
        return days;
    }
}