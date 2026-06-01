package learntime.backend.domain.badge.model;

import jakarta.persistence.*;
import learntime.backend.domain.badge.enums.BadgeType;
import learntime.backend.domain.user.model.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_badge", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_badge", columnNames = {"user_id", "badge_type"})
})
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userBadgeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", nullable = false)
    private BadgeType badgeType;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime acquiredAt;

    @Builder
    public UserBadge(User user, BadgeType badgeType) {
        this.user = user;
        this.badgeType = badgeType;
    }
}
