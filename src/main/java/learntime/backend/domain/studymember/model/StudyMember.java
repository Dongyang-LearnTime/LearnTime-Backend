package learntime.backend.domain.studymember.model;

import jakarta.persistence.*;
import learntime.backend.domain.studymember.enums.StudyMemberRole;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyFeedback;
import learntime.backend.domain.study.model.StudyMemberContent;
import learntime.backend.domain.study.model.StudyStatus;
import learntime.backend.domain.user.model.User;
import lombok.*;
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
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
    private StudyMemberRole studyMemberRole;

    @Builder.Default
    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyMemberContent> studyMemberContents = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyFeedback> studyFeedbacks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyStatus> studyStatuses = new ArrayList<>();

    // --- 비즈니스 로직 --- //
    public void changeRole(StudyMemberRole studyMemberRole) {
        this.studyMemberRole = studyMemberRole;
    }

}
