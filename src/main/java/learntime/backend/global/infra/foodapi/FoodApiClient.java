package learntime.backend.global.infra.foodapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
public class FoodApiClient {

    @Value("${food.api.key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FoodApiClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 식약처 API를 호출하여 100g 당 영양 정보를 반환
     * 결과가 없거나 에러가 발생하면 null을 반환하여 Gemini Fallback 로직을 유도
     */
    public FoodNutrientInfo searchFood(String keyword) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString("https://apis.data.go.kr/1471000/FoodNtrCpntDbInfo02/getFoodNtrCpntDbInq02")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 1)
                    .queryParam("type", "json")
                    .queryParam("FOOD_NM_KR", keyword)
                    .build()
                    .encode()
                    .toUri();

            log.info("API 호출 URI: {}", uri);

            String rawResponse = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            return parseToDto(rawResponse);

        } catch (Exception e) {
            log.error("Food API 호출 실패: {}", e.getMessage());
            return null; // 실패 시 MealService에서 Gemini Fallback 작동
        }
    }

    private FoodNutrientInfo parseToDto(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("body").path("items");

            if (items.isMissingNode() || !items.isArray() || items.isEmpty()) {
                return null;
            }

            JsonNode firstItem = items.get(0);

            // AMT_NUM1: 에너지(kcal), AMT_NUM3: 단백질(g)
            double calories = firstItem.path("AMT_NUM1").asDouble(0.0);
            double protein = firstItem.path("AMT_NUM3").asDouble(0.0);

            return new FoodNutrientInfo(calories, protein);
        } catch (Exception e) {
            log.error("JSON 파싱 에러: {}", e.getMessage());
            return null;
        }
    }

    // 내부에서 사용할 DTO 레코드
    public record FoodNutrientInfo(double caloriesPer100g, double proteinPer100g) {}
}
