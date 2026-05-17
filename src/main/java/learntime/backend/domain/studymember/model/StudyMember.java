package learntime.backend.domain.studymember.model;

import jakarta.persistence.*;
import learntime.backend.domain.study.enums.StudyRole;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyFeedback;
import learntime.backend.domain.study.model.StudyMemberContent;
import learntime.backend.domain.study.model.StudyStatus;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime joinedAt; // 참가일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyRole studyRole;

    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyMemberContent> studyMemberContents = new ArrayList<>();

    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyFeedback> studyFeedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyStatus> studyStatuses = new ArrayList<>();

    @Builder
    public StudyMember(learntime.backend.domain.user.model.User user, Study study, learntime.backend.domain.study.enums.StudyRole studyRole) {
        this.user = user;
        this.study = study;
        this.studyRole = studyRole;
    }

    // --- 비즈니스 로직 --- //

}
