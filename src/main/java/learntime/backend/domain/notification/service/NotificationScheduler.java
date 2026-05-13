package learntime.backend.domain.notification.service;

import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.notification.model.Reminder;
import learntime.backend.domain.notification.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;

    // 매 분 0초마다 쿼리를 날려서 현재 시각에 해당하는 알림이 있는지 파악
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendReminderNotifications() {
        // 1. 현재 시간 (초와 나노초는 무시하고 '분' 단위로 맞춤)
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        // 2. 해당 시간에 발송해야 할 대기 중인 리마인더 조회
        List<Reminder> reminders = reminderRepository.findAllByRemindAtAndStatus(now, Reminder.ReminderStatus.WAITING);

        if (reminders.isEmpty()) return;

        for (Reminder reminder : reminders) {
            CalendarRecord record = reminder.getCalendarRecord();
            Long userId = record.getUser().getUserId();

            // 3. 전송할 데이터 구성 (제목, 시간, 일정ID 포함)
            Map<String, Object> payload = Map.of(
                    "title", record.getTitle(),
                    "targetDate", record.getTargetDate().toString(),
                    "calendarRecordId", record.getCalendarRecordId()
            );

            // 4. SSE를 통해 알림 발송
            notificationService.send(userId, payload, "calendar-reminder");

            // 5. 발송 완료 상태로 변경
            reminder.markAsSent();
        }

        log.info("{}건의 리마인더 알림을 발송했습니다.", reminders.size());
    }
}
