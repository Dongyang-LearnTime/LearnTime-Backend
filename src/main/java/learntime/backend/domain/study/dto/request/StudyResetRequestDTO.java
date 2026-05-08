package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "공부 진도 및 일정 초기화 요청 DTO")
public record StudyResetRequestDTO(
        @NotNull(message = "새로운 시작 날짜는 필수입니다.")
        @FutureOrPresent(message = "시작 날짜는 오늘 이후여야 합니다.")
        LocalDate startDate,

        List<DayOfWeek> restDays,
        List<LocalDate> restDates
) { }
