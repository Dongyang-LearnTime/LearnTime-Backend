package learntime.backend.domain.relationship.model;

import jakarta.persistence.*;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_blocker_blocked",
                        columnNames = {"blocker_id", "blocked_id"}
                )
        },
        indexes = {
                @Index(name = "idx_blocker", columnList = "blocker_id"),
                @Index(name = "idx_blocked", columnList = "blocked_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userBlockId;

    // 차단한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    // 차단 당한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Builder
    public UserBlock(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }

}
