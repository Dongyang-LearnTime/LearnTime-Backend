package learntime.backend.domain.exercise.converter;

import learntime.backend.domain.exercise.dto.request.ExerciseRequestDTO;
import learntime.backend.domain.exercise.dto.request.WeightRequestDTO;
import learntime.backend.domain.exercise.dto.response.ExerciseCalorieResponseDTO;
import learntime.backend.domain.exercise.dto.response.ExerciseResponseDTO;
import learntime.backend.domain.exercise.dto.response.MealResponseDTO;
import learntime.backend.domain.exercise.dto.response.WeightResponseDTO;
import learntime.backend.domain.exercise.model.ExerciseRecord;
import learntime.backend.domain.exercise.model.MealRecord;
import learntime.backend.domain.exercise.model.WeightRecord;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class ExerciseConverter {

    public ExerciseConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static ExerciseRecord toExerciseRecord(User user, ExerciseRequestDTO request, ExerciseCalorieResponseDTO response) {
        return ExerciseRecord.builder()
                .user(user) // 찾은 유저 세팅
                .bodyParts(request.getBodyParts())
                .duration(request.getDuration())
                .content(request.getContent())
                .calories(response.getCalories())
                .build();
    }

    public static ExerciseResponseDTO toExerciseResponseDTO(ExerciseRecord exerciseRecord) {
        return ExerciseResponseDTO.builder()
                .id(exerciseRecord.getExerciseRecordId())
                .bodyParts(exerciseRecord.getBodyParts())
                .duration(exerciseRecord.getDuration())
                .content(exerciseRecord.getContent())
                .calories(exerciseRecord.getCalories())
                .createdAt(exerciseRecord.getCreatedAt())
                .build();
    }

    public static MealRecord toMealRecord(User user, String searchKeyword, int finalCalories, double finalProtein, boolean isEstimated) {
        return MealRecord.builder()
                .user(user)
                .foodName(searchKeyword)
                .calories(finalCalories)
                .protein(finalProtein)
                .isEstimated(isEstimated)
                .build();
    }

    public static MealResponseDTO toMealResponseDTO(MealRecord mealRecord) {
        return MealResponseDTO.builder()
                .id(mealRecord.getMealRecordId())
                .foodName(mealRecord.getFoodName())
                .calories(mealRecord.getCalories())
                .protein(mealRecord.getProtein())
                .isEstimated(mealRecord.getIsEstimated())
                .createdAt(mealRecord.getCreatedAt())
                .build();
    }

    public static WeightRecord toWeightRecord(User user, WeightRequestDTO request) {
        return WeightRecord.builder()
                .user(user)
                .weight(request.getWeight())
                .bodyFat(request.getBodyFat())
                .build();
    }

    public static WeightResponseDTO toWeightResponseDTO(WeightRecord weightRecord) {
        return WeightResponseDTO.builder()
                .id(weightRecord.getWeightRecordId())
                .weight(weightRecord.getWeight())
                .bodyFat(weightRecord.getBodyFat())
                .createdAt(weightRecord.getCreatedAt())
                .build();
    }

}
