package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.error.code.StudyErrorCode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Schema(description = "공부 진도 메타데이터와 목차 정보를 담은 요청 DTO")
public record GeminiStudyRequestDTO(
        @NotNull(message = "책 제목은 필수입니다.")
        @Size(max = 150, message = "책 제목은 150자 이하여야 합니다")
        String bookTitle,

        @NotNull(message = "공부 진도 제목은 필수입니다.")
        @Size(max = 100, message = "진도 제목은 100자 이하여야 합니다")
        String studyTitle,

        @NotNull(message = "시작 날짜는 필수입니다.")
        @FutureOrPresent(message = "시작 날짜는 오늘 이후여야 합니다.")
        LocalDate startDate, // yyyy-MM-dd 형식

        @NotNull(message = "종료 날짜는 필수입니다.")
        @FutureOrPresent(message = "종료 날짜는 오늘 이후여야 합니다.")
        LocalDate endDate,    // yyyy-MM-dd 형식

        @NotEmpty(message = "목차 정보는 최소 한 개 이상 포함되어야 합니다.")
        List <TocListResponseDTO> tocList,

        List <Long> studyMemberList, // 스터디 맴버 리스트

        List<Object> restDays, // 쉬는 요일
        List<LocalDate> restDates // 쉬는 날짜
) {
    public Integer getValidatedStudyDays() {
        if (endDate.isBefore(startDate)) {
            throw new StudyException(StudyErrorCode.INVALID_DATE_RANGE);
        }

        Set<DayOfWeek> restDaySet = (restDays == null || restDays.isEmpty())
                ? Collections.emptySet()
                : restDays.stream()
                        .map(this::parseDayOfWeek)
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));

        Set<LocalDate> restDateSet = (restDates == null || restDates.isEmpty())
                ? Collections.emptySet()
                : new HashSet<>(restDates);

        // 실제 공부 일수 계산 (Stream API 활용)
        int actualStudyDays = (int) startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> !restDaySet.contains(date.getDayOfWeek())) // 요일 제거
                .filter(date -> !restDateSet.contains(date)) // 날짜 제거
                .count();

        // 쉬는 날짜, 쉬는 요일을 뺀 실제 일수에서 계산
        if (actualStudyDays < 14 || actualStudyDays > 90) {
            throw new StudyException(StudyErrorCode.INVALID_STUDY_PERIOD);
        }

        return actualStudyDays;
    }

    public List<DayOfWeek> getRestDaysAsDayOfWeek() {
        if (restDays == null) return Collections.emptyList();
        return restDays.stream()
                .map(this::parseDayOfWeek)
                .filter(Objects::nonNull)
                .toList();
    }

    private DayOfWeek parseDayOfWeek(Object val) {
        if (val == null) return null;
        if (val instanceof Number num) {
            int v = num.intValue();
            if (v >= 0 && v <= 6) {
                return DayOfWeek.of(v + 1);
            }
        } else if (val instanceof String str) {
            String trimmed = str.trim();
            try {
                int v = Integer.parseInt(trimmed);
                if (v >= 0 && v <= 6) {
                    return DayOfWeek.of(v + 1);
                }
            } catch (NumberFormatException e) {
                try {
                    return DayOfWeek.valueOf(trimmed.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
        }
        return null;
    }
}
