package learntime.backend.domain.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.exercise.dto.request.MealRequestDTO;
import learntime.backend.domain.exercise.model.MealRecord;
import learntime.backend.domain.exercise.repository.MealRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.infra.foodapi.FoodApiClient;
import learntime.backend.global.infra.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public MealRecord saveMeal(String email, MealRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        Map<String, Object> geminiRequest = createGeminiMealPrompt(request.getContent());

        try {
            String rawJson = geminiClient.sendRequest(geminiRequest);
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
            MealRecord record = MealRecord.builder()
                    .user(user)
                    .foodName(searchKeyword)
                    .calories(finalCalories)
                    .protein(finalProtein)
                    .isEstimated(isEstimated)
                    .build();

            return mealRecordRepository.save(record);

        } catch (Exception e) {
            log.error("식단 분석 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED_EX);
        }
    }

    private Map<String, Object> createGeminiMealPrompt(String userInput) {
        String prompt = """
                다음 사용자의 식단 입력을 분석해서 JSON으로 반환해줘.
                입력: "%s"
                
                조건:
                1. searchKeyword: 공공데이터 API 검색에 가장 적합한 핵심 음식명 (예: "스팸김치제육도시락" -> "제육도시락" 또는 "제육볶음")
                2. portionInGrams: 사용자가 섭취한 예상 무게(g). 만약 수량이 명시되지 않았다면 무조건 '1인분'을 기준으로 통상적인 무게(g)를 추정할 것. (ml 단위의 음식도 밀도를 무시하고 g으로 취급할 것)
                3. fallbackData: 만약 API 검색에 실패할 경우를 대비해, 이 음식(portionInGrams 기준)의 대략적인 총 칼로리와 단백질량을 제공할 것.
                
                반드시 아래 JSON 구조만 반환할 것:
                {
                  "searchKeyword": "문자열",
                  "portionInGrams": 숫자,
                  "fallbackData": {
                    "calories": 숫자,
                    "protein": 숫자
                  }
                }
                """.formatted(userInput);

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
