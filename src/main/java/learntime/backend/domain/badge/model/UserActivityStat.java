package learntime.backend.domain.badge.model;

import jakarta.persistence.*;
import learntime.backend.domain.badge.enums.StatKey;
import learntime.backend.domain.user.model.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_activity_stat", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_stat", columnNames = {"user_id", "stat_key"})
})
public class UserActivityStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userStatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "stat_key", nullable = false)
    private StatKey statKey;

    @Column(nullable = false)
    private long statValue = 0;

    @Column
    private LocalDate lastActionDate; // 마지막 활동 날

    @Builder
    public UserActivityStat(User user, StatKey statKey) {
        this.user = user;
        this.statKey = statKey;
        this.statValue = 0;
    }

    public void incrementValue() {
        this.statValue++;
    }

    public void resetValueToOne() {
        this.statValue = 1;
    }

    public void resetValueToZero() {
        this.statValue = 0;
    }

    public void updateLastActionDate(LocalDate lastActionDate) {
        this.lastActionDate = lastActionDate;
    }
}
