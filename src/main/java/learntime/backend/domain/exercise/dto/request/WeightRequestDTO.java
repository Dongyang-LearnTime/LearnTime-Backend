package learntime.backend.domain.exercise.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WeightRequestDTO {
    private Double weight;
    private Double bodyFat;
}
