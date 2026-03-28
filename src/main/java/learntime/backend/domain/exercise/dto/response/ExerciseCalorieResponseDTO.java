package learntime.backend.domain.exercise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseCalorieResponseDTO {
    private Integer calories; // Gemini로 부터 응답받을 키 값인 calories와 매칭
}
