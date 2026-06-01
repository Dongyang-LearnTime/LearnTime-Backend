package learntime.backend.domain.exercise.service;

import learntime.backend.domain.exercise.model.ExerciseRecord;
import learntime.backend.domain.exercise.model.MealRecord;
import learntime.backend.domain.exercise.model.WeightRecord;
import learntime.backend.domain.exercise.repository.ExerciseRecordRepository;
import learntime.backend.domain.exercise.repository.MealRecordRepository;
import learntime.backend.domain.exercise.repository.WeightRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisQueryService {

    private final UserRepository userRepository;
    private final ExerciseRecordRepository exerciseRepository;
    private final WeightRecordRepository weightRepository;
    private final MealRecordRepository mealRepository;

    @Transactional(readOnly = true)
    public String getWeeklyDataSummary(Long userId, LocalDateTime now, LocalDateTime sevenDaysAgo) {
        User user = userRepository.findById(userId).
                orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        List<ExerciseRecord> exercises = exerciseRepository.findAllByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, sevenDaysAgo, now);
        List<WeightRecord> weights = weightRepository.findAllByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, sevenDaysAgo, now);
        List<MealRecord> meals = mealRepository.findAllByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, sevenDaysAgo, now);

        return buildDataSummary(exercises, weights, meals);
    }

    private String buildDataSummary(List<ExerciseRecord> exercises, List<WeightRecord> weights, List<MealRecord> meals) {
        String exerciseInfo = exercises.stream()
                .map(e -> String.format("- %s: %d분 소모(%d kcal)", e.getBodyParts(), e.getDuration(), e.getCalories()))
                .collect(Collectors.joining("\n"));

        String weightInfo = weights.stream()
                .map(w -> String.format("- %s: %.1fkg(체지방 %.1f%%)", w.getCreatedAt().toLocalDate(), w.getWeight(), w.getBodyFat()))
                .collect(Collectors.joining("\n"));

        String mealInfo = meals.stream()
                .map(m -> String.format("- %s: %d kcal, 단백질 %.1fg", m.getFoodName(), m.getCalories(), m.getProtein()))
                .collect(Collectors.joining("\n"));

        return """
                [최근 7일 운동 기록]
                %s
                
                [최근 7일 체중 변화]
                %s
                
                [최근 7일 식단 기록]
                %s
                """.formatted(
                exerciseInfo.isEmpty() ? "기록 없음" : exerciseInfo,
                weightInfo.isEmpty() ? "기록 없음" : weightInfo,
                mealInfo.isEmpty() ? "기록 없음" : mealInfo
        );
    }
}
