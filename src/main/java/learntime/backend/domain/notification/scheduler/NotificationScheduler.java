package learntime.backend.domain.notification.scheduler;

import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.enums.ReminderStatus;
import learntime.backend.domain.notification.model.Reminder;
import learntime.backend.domain.notification.repository.NotificationRepository;
import learntime.backend.domain.notification.repository.ReminderRepository;
import learntime.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    // 서버 작동 시 오래된 알림 정리 실행
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupOldNotificationsOnStartup() {
        log.info("[알림 정리] 서버 시작 후 오래된 알림 삭제 작업 실행");
        cleanupOldNotifications();
    }

    // 매 분 0초마다 쿼리를 날려서 현재 시각에 해당하는 알림이 있는지 파악
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendReminderNotifications() {
        try {
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
        } catch (Exception e) {
            log.error("[알림 발송 실패] 스케줄러 처리 중 오류 발생", e);
        }
    }

    // 매일 새벽 2시에 오래된 알림 정리 실행
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOldNotificationsScheduled() {
        log.info("[알림 정리] 정기 스케줄러(새벽 2시) 오래된 알림 삭제 작업 실행");
        cleanupOldNotifications();
    }

    // 읽은 지 한 달이 지난 알림 삭제 로직
    private void cleanupOldNotifications() {
        try {
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
            int deletedCount = notificationRepository.deleteOldReadNotifications(oneMonthAgo);
            log.info("[알림 정리 완료] 읽음 처리 후 30일이 지난 알림 {}건 삭제됨", deletedCount);
        } catch (Exception e) {
            log.error("[알림 정리 실패] 삭제 작업 중 오류 발생", e);
        }
    }}
