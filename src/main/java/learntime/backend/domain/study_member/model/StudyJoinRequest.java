package learntime.backend.domain.study_member.model;

import jakarta.persistence.*;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.enums.StudyJoinRequestStatus;
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
        name = "study_join_request",
        indexes = {
                @Index(name = "idx_join_request_study_status", columnList = "study_id, status"),
                @Index(name = "idx_join_request_user_status", columnList = "requester_user_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StudyJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyJoinRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    // 가입을 요청한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requesterUser;

    // 가입 요청 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudyJoinRequestStatus status;

    // 가입 요청 시간
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 상태 변경 시간
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public StudyJoinRequest(Study study, User requesterUser) {
        this.study = study;
        this.requesterUser = requesterUser;
        this.status = StudyJoinRequestStatus.PENDING;
    }

    public void approve() {
        if (!isPending()) {
            throw new StudyException(StudyErrorCode.JOIN_REQUEST_NOT_PENDING);
        }
        this.status = StudyJoinRequestStatus.APPROVED;
    }

    public void reject() {
        if (!isPending()) {
            throw new StudyException(StudyErrorCode.JOIN_REQUEST_NOT_PENDING);
        }
        this.status = StudyJoinRequestStatus.REJECTED;
    }

    public void cancel() {
        if (!isPending()) {
            throw new StudyException(StudyErrorCode.JOIN_REQUEST_NOT_PENDING);
        }
        this.status = StudyJoinRequestStatus.CANCELED;
    }

    public boolean isPending() {
        return this.status == StudyJoinRequestStatus.PENDING;
    }
}
