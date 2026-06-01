package learntime.backend.domain.notification.model;

import jakarta.persistence.*;
import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.notification.enums.ReminderStatus;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reminderId;

    // 리마인더가 연결된 캘린더 일정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_record_id", nullable = false)
    private CalendarRecord calendarRecord;

    // 실제 알림 발송 예정 시각
    @Column(nullable = false)
    private LocalDateTime remindAt;

    // 리마인더 발송 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReminderStatus status = ReminderStatus.WAITING;

    // 알림 발송 완료 처리
    public void markAsSent() {
        this.status = ReminderStatus.SENT;
    }

    // 일정 수정 시 시간 변경
    public void updateRemindAt(LocalDateTime remindAt) {
        this.remindAt = remindAt;
        this.status = ReminderStatus.WAITING; // 수정 시 알림 다시 대기 상태로
    }
}
