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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long calendarRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 작성자 정보

    @Column(nullable = false)
    private String title; // 일정 제목 (예: 상체 운동 하는 날)

    @Column(columnDefinition = "TEXT")
    private String content; // 상세 내용 (예: 운동 - 벤치프레스 5세트, 식단 - 닭가슴살 샐러드)

    @Column(nullable = false)
    private LocalDateTime targetDate; // 일정 날짜 및 시간

    @Builder.Default
    private Boolean isCompleted = false; // 완료 여부 (기본값 false)

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt; // 생성 시간

    // 일정 수정 기능을 위한 편의 메서드
    public void update(String title, String content, LocalDateTime targetDate, Boolean isCompleted) {
        this.title = title;
        this.content = content;
        this.targetDate = targetDate;
        this.isCompleted = isCompleted;
    }
}