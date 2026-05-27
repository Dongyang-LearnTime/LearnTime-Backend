package learntime.backend.domain.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.exercise.converter.ExerciseConverter;
import learntime.backend.domain.exercise.dto.request.MealRequestDTO;
import learntime.backend.domain.exercise.dto.response.MealResponseDTO;
import learntime.backend.domain.exercise.error.code.ExerciseErrorCode;
import learntime.backend.domain.exercise.error.exception.ExerciseException;
import learntime.backend.domain.exercise.model.MealRecord;
import learntime.backend.domain.exercise.repository.MealRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.infra.foodapi.FoodApiClient;
import learntime.backend.global.infra.gemini.GeminiClient;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class MealService {

    private final MealRecordRepository mealRecordRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final FoodApiClient foodApiClient;
    private final ExercisePromptProvider promptProvider;

    @Transactional
    public MealResponseDTO saveMeal(Long userId, MealRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        Map<String, Object> geminiRequest = createGeminiMealPrompt(request.getContent());

        try {
            String rawJson = geminiClient.sendRequest(geminiRequest, GeminiModel.GEMINI_3_1);
            JsonNode analysis = parseGeminiResponse(rawJson);

            String searchKeyword = analysis.get("searchKeyword").asText();
            int portionInGrams = analysis.get("portionInGrams").asInt();

            int finalCalories;
            double finalProtein;
            boolean isEstimated = false;

            // 2. 공공데이터 API 호출
            FoodApiClient.FoodNutrientInfo apiResult = foodApiClient.searchFood(searchKeyword);

            if (apiResult != null) {
                // API 결과가 있을 경우: 100g 당 데이터이므로 섭취량(portionInGrams)에 비례하여 계산 -> 칼로리와 단백질 표준화
                finalCalories = (int) Math.round((apiResult.caloriesPer100g() * portionInGrams) / 100.0);
                finalProtein = (apiResult.proteinPer100g() * portionInGrams) / 100.0;
                // 소수점 둘째 자리에서 반올림 처리
                finalProtein = Math.round(finalProtein * 10.0) / 10.0;
            } else {
                // 3. API 검색 실패 시: Gemini가 준 Fallback 데이터 사용
                log.info("API 검색 실패. Gemini 예측 데이터(Fallback)를 사용합니다.");
                JsonNode fallback = analysis.get("fallbackData");
                finalCalories = fallback.get("calories").asInt();
                finalProtein = fallback.get("protein").asDouble();
                isEstimated = true;
            }

            // 4. DB 저장
            MealRecord record = ExerciseConverter.toMealRecord(user, searchKeyword, finalCalories, finalProtein, isEstimated);
            MealRecord saveRecord = mealRecordRepository.save(record);

            return ExerciseConverter.toMealResponseDTO(saveRecord);

        } catch (Exception e) {
            log.error("식단 분석 실패: {}", e.getMessage());
            throw new ExerciseException(ExerciseErrorCode.AI_GENERATION_FAILED_EX);
        }
    }

    @Transactional(readOnly = true)
    public List<MealResponseDTO> getTodayMeals(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        LocalDate today = java.time.LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(java.time.LocalTime.MAX);

        List<MealRecord> mealRecords = mealRecordRepository.findAllByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, start, end);

        return mealRecords.stream()
                .map(ExerciseConverter::toMealResponseDTO)
                .toList();
    }

    @Transactional
    public void deleteMealRecord(Long mealRecordId, Long userId) {
        MealRecord mealRecord = mealRecordRepository.findById(mealRecordId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEAL_DATA_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, mealRecord.getUser().getUserId());
        mealRecordRepository.delete(mealRecord);
    }

    private Map<String, Object> createGeminiMealPrompt(String userInput) {
        String prompt = promptProvider.getMealAnalysisPrompt().formatted(userInput);

        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("response_mime_type", "application/json")
        );
    }

    private JsonNode parseGeminiResponse(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);
        String jsonContent = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        return objectMapper.readTree(jsonContent);
    }
}
