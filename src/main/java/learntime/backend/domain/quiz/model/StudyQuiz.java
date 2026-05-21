package learntime.backend.domain.quiz.model;

import jakarta.persistence.*;
import learntime.backend.domain.quiz.enums.QuizStatus;
import learntime.backend.domain.study_member.model.StudyMember;
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
                @Index(name = "idx_study_quiz_member_created", columnList = "study_member_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyQuiz extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyQuizId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_member_id", nullable = false)
    private StudyMember studyMember;

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
    public StudyQuiz(StudyMember studyMember, String quizTitle) {
        this.studyMember = studyMember;
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
