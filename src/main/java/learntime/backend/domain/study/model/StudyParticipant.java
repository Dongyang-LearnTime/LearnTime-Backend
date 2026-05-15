package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import learntime.backend.domain.study.enums.StudyParticipantRole;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "study_participant",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"study_id", "user_id"})
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyParticipantId;

    /** 참가자가 속한 공부 일정 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Study study;

    /** 공부 일정에 참여하는 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /** 공부 일정에서의 역할 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudyParticipantRole role;

    /** 사용자가 공유 공부 일정에서 나간 시각 */
    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "studyParticipant")
    @Builder.Default
    private List<StudyDailyPlan> dailyPlans = new ArrayList<>();

    public boolean isOwner() {
        return this.role == StudyParticipantRole.OWNER;
    }
}
