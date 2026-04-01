package learntime.backend.domain.user.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prompt_quotas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptQuota {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 5")
    private int remainingCount;

    public PromptQuota(User user) {
        this.user = user;
        this.remainingCount = 5; // 할당량 기본 5
    }

}