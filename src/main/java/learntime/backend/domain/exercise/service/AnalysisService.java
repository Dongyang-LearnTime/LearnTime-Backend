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
        String prompt = """
                사용자의 최근 7일 건강 데이터를 바탕으로 맞춤형 분석 제안을 작성해줘.
                운동량, 체중 변화, 그리고 섭취한 영양(칼로리/단백질)의 상관관계를 분석하는 것이 핵심이야. (단, 유의미한 상관관계가 없는 경우 이 지침은 건너뛰어도 좋아.)
                
                데이터:
                %s
                
                요구사항:
                1. 운동, 체중, 식단(칼로리/단백질 밸런스)을 분석해서 칭찬, 개선, 주의 중 적절한 조언 3개를 생성해.
                1-1. 그리고 3개 조언 이외에도 네가 생각하기에 추가하면 좋겠다 싶은 칭찬, 개선, 주의 조언 중 하나를 자유롭게 추가해줘. 이 조언은 통찰력이 없어도 괜찮아.
                (예, 5일 연속 고중량 운동을 진행중입니다. 내일 하루는 스트레칭으로 근육 회복을 도와주세요.)
                2. 단순히 기록을 읽어주는 것이 아니라, "단백질 섭취가 운동량에 비해 부족합니다"와 같이 데이터 기반의 통찰력 있는 조언을 해줘. 실제 수치도 언급하면 좋아.
                (단, 이 부분도 통찰력을 도출 할 수 없는 경우 운동, 체중, 식단에 대한 개별 조언을 해도 문제없어.)
                3. 응답은 반드시 아래 JSON 구조만 지켜서 답변해:
                {
                  "analysis": [
                    { "title": "...", "content": "...", "type": "칭찬/개선/주의" }
                  ]
                }
                4. 만약 7일간의 데이터가 없다면, 그 동안은 매우 간단한 조언이나 사용자에게 7일간의 데이터를 쌓게끔 격려하는 답변을 해줘.
                """.formatted(dataSummary);

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
