package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import learntime.backend.domain.study.enums.QuizStatus;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "study_quiz",
        indexes = {
                @Index(name = "idx_study_quiz_study_id", columnList = "study_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyQuiz extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyQuizId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Column(name = "quiz_title", nullable = false, length = 120)
    private String quizTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_status", length = 20, nullable = false)
    private QuizStatus quizStatus;

    @Column(nullable = false)
    private Integer completedCount = 0;

    // 퀴즈 문제
    @OneToMany(mappedBy = "studyQuiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizQuestion> questions = new ArrayList<>();

    // 퀴즈 풀이 이력
    @OneToMany(mappedBy = "studyQuiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizHistory> quizHistories = new ArrayList<>();

    @Builder
    public StudyQuiz(Study study, String quizTitle) {
        this.study = study;
        this.quizTitle = quizTitle;
        this.quizStatus = QuizStatus.NOT_STARTED;
    }

    public void completeQuiz() {
        this.quizStatus = QuizStatus.COMPLETED;
        this.completedCount++;
    }

    public void setTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

}