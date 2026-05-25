package learntime.backend.domain.calendar.model;

import jakarta.persistence.*;
import learntime.backend.domain.notification.model.Reminder;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "calendar")
public class CalendarRecord extends BaseTimeEntity {

    /** 캘린더 일정 고유 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long calendarRecordId;

    /** 일정을 등록한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 일정 상세 내용 */
    @Column(nullable = false, length = 200)
    private String content;

    /** 일정 날짜 및 시간 */
    @Column(nullable = false)
    private LocalDateTime targetDate;

    /** 일정 완료 여부 */
    @Builder.Default
    private Boolean isCompleted = false;

    /** 중요 일정 여부 */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isImportant = false;

    /** 일정에 연계된 루틴 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    // 리마인더 목록
    @Builder.Default
    @OneToMany(mappedBy = "calendarRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reminder> reminders = new ArrayList<>();

    // 일정 수정 기능을 위한 편의 메서드
    public void update(String content, LocalDateTime targetDate, Boolean isCompleted, Boolean isImportant) {
        this.content = content;
        this.targetDate = targetDate;
        this.isCompleted = isCompleted;
        this.isImportant = isImportant;
    }

    // 리마인더 추가 편의 메서드
    public void addReminder(Reminder reminder) {
        this.reminders.add(reminder);
    }
}
