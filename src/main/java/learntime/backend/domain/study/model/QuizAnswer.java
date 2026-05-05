package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizAnswerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_history_id", nullable = false)
    private QuizHistory quizHistory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion;

    @Column(length = 255)
    private String userAnswer;

    @Column(columnDefinition = "TINYINT(1)", nullable = false)
    private Boolean isCorrect;

    @Builder
    public QuizAnswer(QuizHistory quizHistory, QuizQuestion quizQuestion, String userAnswer, Boolean isCorrect) {
        this.quizHistory = quizHistory;
        this.quizQuestion = quizQuestion;
        this.userAnswer = userAnswer;
        this.isCorrect = isCorrect;
    }
}
