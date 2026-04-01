package learntime.backend.domain.exercise.model;

import jakarta.persistence.*;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WeightRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long weightRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double weight; // 체중 (kg)
    private Double bodyFat; // 체지방률 (%)
}
