package learntime.backend.domain.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.exercise.dto.response.AnalysisResponseDTO;
import learntime.backend.domain.exercise.model.ExerciseRecord;
import learntime.backend.domain.exercise.repository.ExerciseRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.exercise.model.WeightRecord;
import learntime.backend.domain.exercise.repository.WeightRecordRepository;
import learntime.backend.domain.user.repository.UserRepository;
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
    private final PromptQuotaUtil promptQuotaUtil;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AnalysisResponseDTO getWeeklyAnalysis(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        User user = userRepository.findById(userId).
                orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 1. 최근 7일간의 운동 및 체중 데이터 조회
        List<ExerciseRecord> exercises = exerciseRepository.findAllByUserAndCreateAtBetweenOrderByCreateAtAsc(user, sevenDaysAgo, now);
        List<WeightRecord> weights = weightRepository.findAllByUserAndCreateAtBetweenOrderByCreateAtAsc(user, sevenDaysAgo, now);

        // 2. AI에게 전달할 데이터 요약 생성
        String dataSummary = buildDataSummary(exercises, weights);
        promptQuotaUtil.decreasePromptQuota(userId); // Gemini 이용량 차감

        // 3. Gemini 요청 바디 생성
        Map<String, Object> requestBody = createAnalysisRequest(dataSummary);

        try {
            // 4. Gemini API 호출
            String rawJson = geminiClient.sendRequest(requestBody);

            // 5. 응답 파싱
            return parseAnalysisResponse(rawJson);
        } catch (Exception e) {
            log.error("AI 분석 생성 중 오류 발생: {}", e.getMessage());
            promptQuotaUtil.restorePromptQuota(userId);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private String buildDataSummary(List<ExerciseRecord> exercises, List<WeightRecord> weights) {
        String exerciseInfo = exercises.stream()
                .map(e -> String.format("- %s: %d분 소모(%d kcal)", e.getBodyParts(), e.getDuration(), e.getCalories()))
                .collect(Collectors.joining("\n"));

        String weightInfo = weights.stream()
                .map(w -> String.format("- %s: %.1fkg(체지방 %.1f%%)", w.getCreateAt().toLocalDate(), w.getWeight(), w.getBodyFat()))
                .collect(Collectors.joining("\n"));

        return """
                [최근 7일 운동 기록]
                %s
                
                [최근 7일 체중 변화]
                %s
                """.formatted(exerciseInfo.isEmpty() ? "기록 없음" : exerciseInfo,
                weightInfo.isEmpty() ? "기록 없음" : weightInfo);
    }

    private Map<String, Object> createAnalysisRequest(String dataSummary) {
        String prompt = """
                사용자의 최근 7일 건강 데이터를 바탕으로 맞춤형 분석 제안을 작성해줘.
                
                데이터:
                %s
                
                요구사항:
                1. 운동량, 체중 변화 추이를 분석해서 칭찬, 개선, 주의 중 적절한 조언 3개를 생성해.
                2. 응답은 반드시 아래 JSON 구조만 지켜서 답변해:
                {
                  "analysis": [
                    { "title": "...", "content": "...", "type": "칭찬/개선/주의" }
                  ]
                }
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
