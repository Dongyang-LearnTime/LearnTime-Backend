package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "study_daily_plan",
        indexes = {
                // 복합 인덱스: study_id로 조회 후 day_number로 정렬하는 쿼리(O(log N))를 최적화
                @Index(name = "idx_study_daily_plan_on_study_and_day", columnList = "study_id, day_number")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyDailyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyDailyPlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Column(name = "day_number", nullable = false)
    private int dayNumber; // DTO의 day

    @Column(name = "plan_content", nullable = false)
    private String planContent;

    @Builder
    public StudyDailyPlan(int dayNumber, String planContent) {
        this.dayNumber = dayNumber;
        this.planContent = planContent;
    }

}