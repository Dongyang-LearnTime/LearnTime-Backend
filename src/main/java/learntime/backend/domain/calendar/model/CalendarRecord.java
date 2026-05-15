package learntime.backend.domain.calendar.model;

import jakarta.persistence.*;
import learntime.backend.domain.user.model.User;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "calendar")
public class CalendarRecord {

    /** 캘린더 일정 고유 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long calendarRecordId;

    /** 일정을 등록한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 일정 제목 */
    @Column(nullable = false)
    private String title;

    /** 일정 상세 내용 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 일정 날짜 및 시간 */
    @Column(nullable = false)
    private LocalDateTime targetDate;

    /** 일정 완료 여부 */
    @Builder.Default
    private Boolean isCompleted = false;

    /** 일정 생성 시각 */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 일정 수정 기능을 위한 편의 메서드
    public void update(String title, String content, LocalDateTime targetDate, Boolean isCompleted) {
        this.title = title;
        this.content = content;
        this.targetDate = targetDate;
        this.isCompleted = isCompleted;
    }
}
