package learntime.backend.domain.user.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_user_id", columnList = "userId") // DB 성능 최적화를 위한 인덱스
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptQuotas {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 5")
    private int remainingCount;

    @Column()
    private LocalDateTime exhaustedAt; // 할당량 소진 된 시간

    public PromptQuotas(User user, int maxQuota) {
        this.user = user;
        this.remainingCount = maxQuota;
    }

}