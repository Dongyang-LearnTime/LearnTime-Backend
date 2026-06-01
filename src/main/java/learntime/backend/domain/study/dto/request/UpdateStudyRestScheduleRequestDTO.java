package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "공부 진도 휴무 일정 재조정 요청 DTO")
public record UpdateStudyRestScheduleRequestDTO(
        List<DayOfWeek> restDays,
        List<LocalDate> restDates
) {
}
