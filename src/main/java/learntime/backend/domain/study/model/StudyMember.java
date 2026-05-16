package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.quiz.model.StudyQuiz;
import learntime.backend.domain.study.enums.StudyRole;
import learntime.backend.domain.user.model.User;
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
@Table(
        name = "study_member",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"study_id", "user_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StudyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyRole studyRole;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime joinedAt; // 참가일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    // 진도 AI 피드백
    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyFeedback> studyFeedbacks = new ArrayList<>();

    // 퀴즈
    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyQuiz> studyQuizzes = new ArrayList<>();

    // 필기 (SET NULL)
    @OneToMany(mappedBy = "studyMember")
    private List<StudyNotes> studyNotes = new ArrayList<>();

    // 공부 일일 진도 상태
    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyStatus> studyStatuses = new ArrayList<>();

    // 일일 진도 맴버 추가 내용
    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyMemberContent> studyMemberContents = new ArrayList<>();

    @Builder
    public StudyMember(StudyRole studyRole, User user, Study study) {
        this.studyRole = studyRole;
        this.user = user;
        this.study = study;
    }

}
