package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "study_user_content")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyUserContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyUserContentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_daily_plan_id", nullable = false)
    private StudyDailyPlan studyDailyPlan;

    @Column(name = "user_content", nullable = false, length = 150)
    private String userContent;

    @Builder
    public StudyUserContent(StudyDailyPlan studyDailyPlan, String userContent) {
        this.studyDailyPlan = studyDailyPlan;
        this.userContent = userContent;
    }

}
