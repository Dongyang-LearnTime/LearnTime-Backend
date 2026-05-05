package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuizHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_quiz_id", nullable = false)
    private StudyQuiz studyQuiz;

    @Column(nullable = false)
    private Integer attemptNumber; // 몇 회차 풀이인지

    @Column(nullable = false)
    private Integer correctCount; // 정답 개수

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "quizHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers = new ArrayList<>();

    @Builder
    public QuizHistory(StudyQuiz studyQuiz, Integer attemptNumber, Integer correctCount) {
        this.studyQuiz = studyQuiz;
        this.attemptNumber = attemptNumber;
        this.correctCount = correctCount;
    }
}
