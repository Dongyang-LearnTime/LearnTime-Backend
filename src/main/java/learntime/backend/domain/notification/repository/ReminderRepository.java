package learntime.backend.domain.notification.repository;

import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.notification.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    void deleteByCalendarRecord(CalendarRecord calendarRecord);

    List<Reminder> findAllByRemindAtAndStatus(LocalDateTime remindAt, Reminder.ReminderStatus status);
}
