package learntime.backend.domain.notification.service;

import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.notification.model.Reminder;
import learntime.backend.domain.notification.model.ReminderOption;
import learntime.backend.domain.notification.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;

    @Transactional
    public void upsertReminder(CalendarRecord record, ReminderOption option) {
        // 기존 리마인더 삭제 (수정 시에도 삭제 후 생성하는 방식으로 작동함)
        reminderRepository.deleteByCalendarRecord(record);

        // 알림이 필요 없는 경우 즉시 종료
        if (option == null || option == ReminderOption.NONE) return;

        LocalDateTime remindAt = Objects.requireNonNull(
                option.calculateRemindAt(record.getTargetDate()),
                        "알림 예정 시각 계산 결과가 null입니다."
                ).withSecond(0).withNano(0);

        reminderRepository.save(Reminder.builder()
                .calendarRecord(record)
                .remindAt(remindAt)
                .build());
    }

    @Transactional
    public void deleteReminder(CalendarRecord record) {
        reminderRepository.deleteByCalendarRecord(record);
    }
}
