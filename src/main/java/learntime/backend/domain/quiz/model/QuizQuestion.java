package learntime.backend.domain.quiz.model;

import jakarta.persistence.*;
import learntime.backend.domain.quiz.enums.QuizType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "quiz_question",
        indexes = {
                @Index(name = "idx_question_study_quiz_id", columnList = "study_quiz_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizQuestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_quiz_id", nullable = false)
    private StudyQuiz studyQuiz;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionContent; // 문제 내용

    @Column(nullable = false, length = 255)
    private String correctAnswer; // 문제 정답

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizType quizType;

    @OneToMany(mappedBy = "quizQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers = new ArrayList<>();

    // --- 연관관계 편의 메서드 --- //

    public void addAnswer(QuizAnswer answer) {
        this.answers.add(answer);
    }

    @Builder
    public QuizQuestion(StudyQuiz studyQuiz, String questionContent, String correctAnswer, QuizType quizType) {
        this.studyQuiz = studyQuiz;
        this.questionContent = questionContent;
        this.correctAnswer = correctAnswer;
        this.quizType = quizType;
    }

}
