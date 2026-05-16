package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "study_daily_plan",
        indexes = {
                // 스터디의 전체 일정을 일차별로 조회 및 정렬
                @Index(name = "idx_study_daily_plan_study_date", columnList = "study_id, plan_date")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyDailyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyDailyPlanId;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber; // DTO의 day

    // 계획 날짜
    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "plan_content", nullable = false)
    private String planContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    // 사용자 작성 내용
    @OneToMany(mappedBy = "studyDailyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyMemberContent> studyMemberContents = new ArrayList<>();

    // 공부 일일 진도 상태
    @OneToMany(mappedBy = "studyDailyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyStatus> studyStatuses = new ArrayList<>();

    @Builder
    public StudyDailyPlan(Study study, Integer dayNumber, LocalDate planDate, String planContent) {
        this.study = study;
        this.dayNumber = dayNumber;
        this.planDate = planDate;
        this.planContent = planContent;
    }

}
