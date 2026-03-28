package learntime.backend.domain.exercise.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseRequestDTO {
    // 받을 운동 정보 객체 생성
    private List<String> bodyParts;
    private Integer duration;
    private String content;
}
