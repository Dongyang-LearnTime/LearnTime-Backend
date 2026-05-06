package learntime.backend.domain.exercise.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MealResponseDTO {
    private Long id;
    private String foodName;
    private Integer calories;
    private Double protein;
    private Boolean isEstimated;
    private LocalDateTime createdAt;
}
