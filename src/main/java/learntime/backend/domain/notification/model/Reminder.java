package learntime.backend.domain.notification.model;

import jakarta.persistence.*;
import learntime.backend.domain.calendar.model.CalendarRecord;
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

    // 어떤 일정에 대한 알림인지 연결 (User 정보는 CalendarRecord를 통해 알 수 있음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_record_id", nullable = false)
    private CalendarRecord calendarRecord;

    @Column(nullable = false)
    private LocalDateTime remindAt; // 실제 알림 발송 예정 시각

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReminderStatus status = ReminderStatus.WAITING;

    public enum ReminderStatus {
        WAITING, SENT, CANCEL
    }

    // 알림 발송 완료 처리
    public void markAsSent() {
        this.status = ReminderStatus.SENT;
    }

    // 일정 수정 시 시간 변경을 위한 메서드
    // 현재의 방식은 일정을 지우고, 재생성하는 방식을 채택 (단순 및 오류발생 적음)
    // 추후 조회 후 수정 방식으로 바꿀 경우 아래의 메서드 사용
    public void updateRemindAt(LocalDateTime remindAt) {
        this.remindAt = remindAt;
        this.status = ReminderStatus.WAITING; // 수정 시 알림 다시 대기 상태로
    }
}
