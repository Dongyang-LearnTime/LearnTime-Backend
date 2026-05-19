package learntime.backend.domain.calendar.dto.event;

import learntime.backend.domain.calendar.model.CalendarRecord;

public record CalendarReminderUpsertEvent(
        CalendarRecord calendarRecord
) {
}
