package learntime.backend.domain.exercise.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MealRequestDTO {
    private String content; // 사용자의 입력 예: 제육볶음 먹었어.
}
