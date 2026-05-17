package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "study_daily_plan",
        indexes = {
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

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private LocalDate planDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String planContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @OneToMany(mappedBy = "studyDailyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyStatus> studyStatuses = new ArrayList<>();

    @Builder
    public StudyDailyPlan(Integer dayNumber, LocalDate planDate, String planContent, Study study) {
        this.dayNumber = dayNumber;
        this.planDate = planDate;
        this.planContent = planContent;
        this.study = study;
    }

}
