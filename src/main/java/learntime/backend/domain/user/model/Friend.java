package learntime.backend.domain.user.model;

import jakarta.persistence.*;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "friend",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "friend_user_id"})
        },
        indexes = {
                @Index(
                        name = "idx_friend_friend_user",
                        columnList = "friend_user_id, user_id"
                )
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friend extends BaseTimeEntity {

    /**
     * 친구 관계 고유 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long friendId;

    /**
     * 자신 (친구를 맺은 사람)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /**
     * 상대방 (내 친구)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User friendUser;
}
