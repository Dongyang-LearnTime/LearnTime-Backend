package learntime.backend.domain.exercise.model;


import jakarta.persistence.*;
import learntime.backend.domain.user.model.User;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "meal")
public class MealRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_id")
    private Long MealRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String foodName; // 최종 결정된 음식명 (예: 제육볶음)

    @Column(nullable = false)
    private Integer calories; // 계산된 총 칼로리

    @Column(nullable = false)
    private Double protein; // 계산된 총 단백질 (g)

    @Column(nullable = false)
    private Boolean isEstimated; // API 실패로 Gemini 예측값을 썼는지 여부

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createAt;
}
