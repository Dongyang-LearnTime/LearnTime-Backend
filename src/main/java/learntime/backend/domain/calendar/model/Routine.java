package learntime.backend.domain.calendar.model;

import jakarta.persistence.*;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "routine")
public class Routine extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long routineId;

    /** 루틴을 등록한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 루틴 상세 내용 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 루틴 일정이 발송/진행될 시작 시각 */
    @Column(nullable = false)
    private LocalTime startTime;

    /** 루틴 시작일 */
    @Column(nullable = false)
    private LocalDate startDate;

    /** 루틴 종료일 (null 이면 무기한 반복) */
    private LocalDate endDate;

    /** 중요 루틴 여부 */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isImportant = false;

    /** 반복 요일 목록 */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "routine_days",
            joinColumns = @JoinColumn(name = "routine_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    @Builder.Default
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    /** 해당 루틴으로 인해 생성된 실제 캘린더 일정 목록 */
    @Builder.Default
    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CalendarRecord> calendarRecords = new ArrayList<>();

    // 루틴 정보 수정 메서드
    public void update(String content, LocalTime startTime, LocalDate startDate, LocalDate endDate, Boolean isImportant, Set<DayOfWeek> daysOfWeek) {
        this.content = content;
        this.startTime = startTime;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isImportant = isImportant;
        this.daysOfWeek = daysOfWeek;
    }
}
