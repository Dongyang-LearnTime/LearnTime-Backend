package learntime.backend.domain.exercise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class WeightResponseDTO {
    private Long id;
    private Double weight;
    private Double bodyFat;
    private LocalDateTime createdAt;
}
