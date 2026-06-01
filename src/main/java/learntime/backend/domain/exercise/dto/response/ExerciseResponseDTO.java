package learntime.backend.domain.exercise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ExerciseResponseDTO {
    private Long id;
    private List<String> bodyParts;
    private Integer duration;
    private String content;
    private Double weight;
    private Integer calories; // AI가 계산한 결과 포함
    private LocalDateTime createdAt;
}
