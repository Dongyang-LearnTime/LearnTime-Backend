package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "study_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyFeedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Column(nullable = false, length = 150)
    private String feedbackTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String feedbackContent;

    @Builder
    public StudyFeedback(Study study, String feedbackTitle, String feedbackContent) {
        this.study = study;
        this.feedbackTitle = feedbackTitle;
        this.feedbackContent = feedbackContent;
    }

    public void updateTitle(String feedbackTitle) {
        this.feedbackTitle = feedbackTitle;
    }
}
