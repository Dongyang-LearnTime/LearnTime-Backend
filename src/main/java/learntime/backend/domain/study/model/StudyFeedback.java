package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "study_feedback",
        indexes = {
                @Index(name = "idx_study_feedback_member_created", columnList = "study_member_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyFeedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_member_id", nullable = false)
    private StudyMember studyMember;

    @Column(nullable = false, length = 150)
    private String feedbackTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String feedbackContent;

    @Builder
    public StudyFeedback(StudyMember studyMember, String feedbackTitle, String feedbackContent) {
        this.studyMember = studyMember;
        this.feedbackTitle = feedbackTitle;
        this.feedbackContent = feedbackContent;
    }

    public void updateTitle(String feedbackTitle) {
        this.feedbackTitle = feedbackTitle;
    }
}
