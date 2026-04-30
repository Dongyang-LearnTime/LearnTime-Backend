package learntime.backend.domain.point.model;

import jakarta.persistence.*;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pointHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer amount; // 포인트 변동 (+100, -50 등)

    @Enumerated(EnumType.STRING)
    private PointType pointType; // EARN(적립), USE(사용), CANCEL(취소)

    @Column(nullable = false, length = 100)
    private String description;

    @Builder
    public PointHistory(int amount, PointType pointType, String description, User user) {
        this.amount = amount;
        this.pointType = pointType;
        this.description = description;
        this.user = user;
    }

}
