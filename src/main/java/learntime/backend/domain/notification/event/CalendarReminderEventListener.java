package learntime.backend.domain.notification.event;

import learntime.backend.domain.calendar.dto.event.CalendarReminderDeleteEvent;
import learntime.backend.domain.calendar.dto.event.CalendarReminderUpsertEvent;
import learntime.backend.domain.notification.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CalendarReminderEventListener {

    private final ReminderService reminderService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleCalendarReminderUpsert(CalendarReminderUpsertEvent event) {
        // 캘린더 저장/수정 트랜잭션 안에서 리마인더를 함께 갱신합니다.
        reminderService.upsertReminder(event.calendarRecord());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleCalendarReminderDelete(CalendarReminderDeleteEvent event) {
        // 캘린더 삭제 전 리마인더를 먼저 삭제해 FK 제약 위반을 방지합니다.
        reminderService.deleteReminder(event.calendarRecord());
    }
}
