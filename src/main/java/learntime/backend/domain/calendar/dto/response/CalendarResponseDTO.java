package learntime.backend.domain.calendar.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import learntime.backend.domain.calendar.model.CalendarRecord;
import java.time.LocalDateTime;

@Builder
@Schema(description = "캘린더 일정 응답 DTO")
public record CalendarResponseDTO (
    @Schema(description = "일정 ID", example = "1")
    Long calendarRecordId,

    @Schema(description = "일정 상세 내용", example = "백준 3문제 풀이")
    String content,

    @Schema(description = "일정 날짜 및 시간")
    LocalDateTime targetDate,

    @Schema(description = "중요 일정 여부", example = "false")
    Boolean isImportant,

    @Schema(description = "생성 시간")
    LocalDateTime createdAt
) { }
