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

    private final PromptQuotaUtil promptQuotaUtil;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final ExercisePromptProvider promptProvider;
    private final AnalysisQueryService analysisQueryService;

    public AnalysisResponseDTO getWeeklyAnalysis(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        // 1. 최근 7일간의 데이터 수집 (운동, 체중, 식단) 및 요약 텍스트 생성 (Read-Only 트랜잭션 내부 수행)
        String dataSummary = analysisQueryService.getWeeklyDataSummary(userId, now, sevenDaysAgo);

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
