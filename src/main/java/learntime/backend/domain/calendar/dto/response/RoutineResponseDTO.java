package learntime.backend.domain.calendar.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import learntime.backend.domain.calendar.model.Routine;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Set;

@Builder
@Schema(description = "루틴 정보 응답 DTO")
public record RoutineResponseDTO(
    @Schema(description = "루틴 ID", example = "1")
    Long routineId,

    @Schema(description = "루틴 상세 내용", example = "매주 알고리즘 문제 풀이 및 깃허브 업로드")
    String content,

    @Schema(description = "루틴 시작 시각")
    LocalTime startTime,

    @Schema(description = "루틴 시작일")
    LocalDate startDate,

    @Schema(description = "루틴 종료일")
    LocalDate endDate,

    @Schema(description = "중요 루틴 여부")
    Boolean isImportant,

    @Schema(description = "반복 요일 목록")
    Set<DayOfWeek> daysOfWeek,

    @Schema(description = "생성 일시")
    LocalDateTime createdAt
) {
}
