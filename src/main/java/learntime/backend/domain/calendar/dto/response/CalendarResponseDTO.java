package learntime.backend.domain.calendar.dto.response;

import lombok.Builder;
import learntime.backend.domain.calendar.model.CalendarRecord;
import java.time.LocalDateTime;

@Builder
public record CalendarResponseDTO (
    Long calendarRecordId,
    String title,
    String content,
    LocalDateTime targetDate,
    Boolean isCompleted,
    LocalDateTime createAt
) {
    public static CalendarResponseDTO from(CalendarRecord record) {
        return CalendarResponseDTO.builder()
                .calendarRecordId(record.getCalendarRecordId())
                .title(record.getTitle())
                .content(record.getContent())
                .targetDate(record.getTargetDate())
                .isCompleted(record.getIsCompleted())
                .createAt(record.getCreateAt())
                .build();

    }
}
