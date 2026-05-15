package learntime.backend.domain.notification.scheduler;

import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.enums.ReminderStatus;
import learntime.backend.domain.notification.model.Reminder;
import learntime.backend.domain.notification.repository.ReminderRepository;
import learntime.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
        // 현재 시간 (초와 나노초는 무시하고 '분' 단위로 맞춤)
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        // 발송 시각이 지났지만 아직 대기 중인 리마인더까지 함께 조회
        List<Reminder> reminders = reminderRepository.findAllByRemindAtLessThanEqualAndStatus(
                now,
                ReminderStatus.WAITING
        );

        if (reminders.isEmpty()) return;

        for (Reminder reminder : reminders) {
            CalendarRecord record = reminder.getCalendarRecord();
            Long userId = record.getUser().getUserId();

            // 알림을 저장하고, 접속 중인 사용자에게는 SSE로 즉시 전송
            notificationService.notify(
                    userId,
                    NotificationType.CALENDAR_REMINDER,
                    "일정 알림",
                    record.getTitle() + " 일정 시간이 다가왔습니다.",
                    record.getCalendarRecordId(),
                    "CALENDAR_RECORD"
            );

            // 발송 완료 상태로 변경
            reminder.markAsSent();
        }

        log.info("{}건의 리마인더 알림을 발송했습니다.", reminders.size());
    }
}
