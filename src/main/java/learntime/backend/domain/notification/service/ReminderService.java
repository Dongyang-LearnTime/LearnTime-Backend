package learntime.backend.domain.notification.service;

import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.notification.model.Reminder;
import learntime.backend.domain.notification.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
// 캘린더 일정의 리마인더 예약 생성, 갱신, 삭제를 담당하는 Service
public class ReminderService {

    private final ReminderRepository reminderRepository;

    @Transactional
    public void upsertReminder(CalendarRecord record) {
        LocalDateTime remindAt = record.getTargetDate().withSecond(0).withNano(0);

        if (record.getReminders() != null && !record.getReminders().isEmpty()) {
            Reminder existing = record.getReminders().get(0);
            existing.updateRemindAt(remindAt);
            return;
        }

        Reminder reminder = Reminder.builder()
                .calendarRecord(record)
                .remindAt(remindAt)
                .build();
        record.addReminder(reminder);

        reminderRepository.save(reminder);
    }

    @Transactional
    public void deleteReminder(CalendarRecord record) {
        reminderRepository.deleteByCalendarRecord(record);
    }

    @Transactional
    public void deleteRemindersByRoutineAndAfter(learntime.backend.domain.calendar.model.Routine routine, LocalDateTime after) {
        reminderRepository.deleteByRoutineAndTargetDateAfter(routine, after);
    }
}
