package learntime.backend.domain.exercise.model;

import jakarta.persistence.*;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ExerciseRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long exerciseRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "exercise_parts", joinColumns = @JoinColumn(name = "exercise_record_id"))
    @Column(name = "part")
    private List<String> bodyParts; // 선택한 운동 부위 리스트 (가슴, 등 등)

    @Column(nullable = false)
    private Integer duration; // 운동 시간 (분)

    @Column(columnDefinition = "TEXT")
    private String content; // 운동 상세 내용 (벤치프레스 5x5 등)

    private Integer calories; // AI(Gemini)가 계산한 소모 칼로리

}
