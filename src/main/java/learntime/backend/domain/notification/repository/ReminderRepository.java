package learntime.backend.domain.notification.repository;

import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.notification.enums.ReminderStatus;
import learntime.backend.domain.notification.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// 캘린더 리마인더의 예약, 삭제, 발송 대상 조회를 담당하는 Repository
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    void deleteByCalendarRecord(CalendarRecord calendarRecord);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Reminder r WHERE r.calendarRecord.user.userId = :userId")
    void deleteAllByCalendarUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Reminder r WHERE r.calendarRecord.routine = :routine AND r.calendarRecord.targetDate > :after")
    void deleteByRoutineAndTargetDateAfter(@Param("routine") learntime.backend.domain.calendar.model.Routine routine, @Param("after") LocalDateTime after);

    List<Reminder> findAllByRemindAtLessThanEqualAndStatus(LocalDateTime remindAt, ReminderStatus status);
}
