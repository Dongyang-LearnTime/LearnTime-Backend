package learntime.backend.domain.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.exercise.dto.response.AnalysisResponseDTO;
import learntime.backend.domain.exercise.model.ExerciseRecord;
import learntime.backend.domain.exercise.model.MealRecord;
import learntime.backend.domain.exercise.repository.ExerciseRecordRepository;
import learntime.backend.domain.exercise.repository.MealRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.exercise.model.WeightRecord;
import learntime.backend.domain.exercise.repository.WeightRecordRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.infra.gemini.GeminiClient;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final UserRepository userRepository;
    private final ExerciseRecordRepository exerciseRepository;
    private final WeightRecordRepository weightRepository;
    private final MealRecordRepository mealRepository;
    private final PromptQuotaUtil promptQuotaUtil;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final ExercisePromptProvider promptProvider;

    @Transactional(readOnly = true)
    public AnalysisResponseDTO getWeeklyAnalysis(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        User user = userRepository.findById(userId).
                orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 1. 최근 7일간의 데이터 수집 (운동, 체중, 식단)
        List<ExerciseRecord> exercises = exerciseRepository.findAllByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, sevenDaysAgo, now);
        List<WeightRecord> weights = weightRepository.findAllByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, sevenDaysAgo, now);
        List<MealRecord> meals = mealRepository.findAllByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, sevenDaysAgo, now);

        // 2. AI에게 전달할 데이터 요약 생성
        String dataSummary = buildDataSummary(exercises, weights, meals);

        promptQuotaUtil.decreasePromptQuota(userId); // Gemini 이용량 차감

        // 3. Gemini 요청 바디 생성
        Map<String, Object> requestBody = createAnalysisRequest(dataSummary);

        try {
            // 4. Gemini API 호출
            String rawJson = geminiClient.sendRequest(requestBody, GeminiModel.GEMINI_3_1);

            // 5. 응답 파싱
            return parseAnalysisResponse(rawJson);

        } catch (Exception e) {
            log.error("AI 분석 생성 중 오류 발생: {}", e.getMessage());
            promptQuotaUtil.restorePromptQuota(userId);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
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

    private Map<String, Object> createAnalysisRequest(String dataSummary) {
        String prompt = promptProvider.getWeeklyAnalysisPrompt().formatted(dataSummary);

        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("response_mime_type", "application/json")
        );
    }

    private AnalysisResponseDTO parseAnalysisResponse(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);
        // 에러 확인
        if (root.has("error")) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }

        String jsonContent = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        return objectMapper.readValue(jsonContent, AnalysisResponseDTO.class);
    }
}
