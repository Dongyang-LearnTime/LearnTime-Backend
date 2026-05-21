package learntime.backend.domain.study_member.model;

import jakarta.persistence.*;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.enums.StudyInvitationStatus;
import learntime.backend.domain.user.model.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "study_invitation",
        uniqueConstraints = {
                @UniqueConstraint(
                        // 같은 맴버 초대 방지
                        name = "uk_study_invitation_study_invited_user",
                        columnNames = {"study_id", "invited_user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_invitation_study_invited_status", columnList = "study_id, invited_user_id, status"),
                @Index(name = "idx_invitation_inviter_user", columnList = "inviter_user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StudyInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyInvitationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    // 초대 받은 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_user_id", nullable = false)
    private User invitedUser;

    // 초대한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_user_id", nullable = false)
    private User inviterUser;

    // 초대 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudyInvitationStatus status;

    // 초대 요청 시간
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    // 초대 상태 수정 시간 (거절/취소 등 상태 변경 시간)
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public StudyInvitation(Study study, User invitedUser, User inviterUser) {
        // 자기 자신 초대 방지
        if (invitedUser.getUserId().equals(inviterUser.getUserId())) {
            throw new StudyException(StudyErrorCode.SELF_INVITATION_NOT_ALLOWED);
        }

        this.study = study;
        this.invitedUser = invitedUser;
        this.inviterUser = inviterUser;
        this.status = StudyInvitationStatus.PENDING;
    }

    public void accept() {
        this.status = StudyInvitationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = StudyInvitationStatus.REJECTED;
    }

    public void cancel() {
        this.status = StudyInvitationStatus.CANCELED;
    }

    // 대기 상태인지 확인
    public boolean isPending() {
        return this.status == StudyInvitationStatus.PENDING;
    }

}