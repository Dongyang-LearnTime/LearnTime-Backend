package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Schema(description = "공부 진도 수정 정보를 담은 요청 DTO")
public record GeminiReplanRequestDTO (
        @NotNull(message = "공부 진도 제목은 필수입니다.")
        @Size(max = 100, message = "진도 제목은 100자 이하여야 합니다")
        String studyTitle,

        @NotNull(message = "시작 날짜는 필수입니다.")
        @FutureOrPresent(message = "시작 날짜는 오늘 이후여야 합니다.")
        LocalDate startDate, // yyyy-MM-dd 형식

        @NotNull(message = "종료 날짜는 필수입니다.")
        LocalDate endDate,    // yyyy-MM-dd 형식

        List<DayOfWeek> restDays, // 쉬는 요일 (비어도 됨)
        List<LocalDate> restDates // 쉬는 날짜 (비어도 됨)
) { }