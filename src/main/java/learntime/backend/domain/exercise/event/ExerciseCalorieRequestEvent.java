package learntime.backend.domain.exercise.event;

import learntime.backend.domain.exercise.dto.request.ExerciseRequestDTO;

public record ExerciseCalorieRequestEvent(
        Long exerciseRecordId,
        ExerciseRequestDTO request
) {
}
