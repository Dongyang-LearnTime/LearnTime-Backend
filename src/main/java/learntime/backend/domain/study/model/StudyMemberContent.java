package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "study_member_content")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyMemberContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyMemberContentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_member_id", nullable = false)
    private StudyMember studyMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_daily_plan_id", nullable = false)
    private StudyDailyPlan studyDailyPlan;

    @Column(nullable = false, length = 150)
    private String memberContent;

    @Builder
    public StudyMemberContent(StudyMember studyMember, StudyDailyPlan studyDailyPlan, String memberContent) {
        this.studyMember = studyMember;
        this.studyDailyPlan = studyDailyPlan;
        this.memberContent = memberContent;
    }

}
