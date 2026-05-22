package learntime.backend.domain.user.model;

import jakarta.persistence.*;
import learntime.backend.domain.user.enums.FriendRequestStatus;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "friend_request")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest extends BaseTimeEntity {

    /**
     * 친구 요청 고유 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long friendRequestId;

    /**
     * 친구 요청을 보낸 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User requester;

    /**
     * 친구 요청을 받은 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User receiver;

    /**
     * 친구 요청 상태 (대기, 수락, 거절)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendRequestStatus status;

    /**
     * 상태 변경 시간 (거절 시간 추적용)
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 친구 요청 수락 상태로 변경
    public void accept() {
        this.status = FriendRequestStatus.ACCEPTED;
    }

    // 친구 요청 거절 상태로 변경
    public void reject() {
        this.status = FriendRequestStatus.REJECTED;
    }

    // 친구 요청 취소 상태로 변경 (요청자가 직접 취소)
    public void cancel() {
        this.status = FriendRequestStatus.CANCELLED;
    }

}
