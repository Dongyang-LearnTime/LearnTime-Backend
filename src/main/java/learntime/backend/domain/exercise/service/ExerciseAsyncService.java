package learntime.backend.domain.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.exercise.dto.request.ExerciseRequestDTO;
import learntime.backend.domain.exercise.dto.response.ExerciseCalorieResponseDTO;
import learntime.backend.domain.exercise.error.code.ExerciseErrorCode;
import learntime.backend.domain.exercise.error.exception.ExerciseException;
import learntime.backend.domain.exercise.event.ExerciseCalorieRequestEvent;
import learntime.backend.domain.exercise.repository.ExerciseRecordRepository;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.global.infra.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseAsyncService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final ExercisePromptProvider promptProvider;
    private final ExerciseStoreService exerciseStoreService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCalorieRequest(ExerciseCalorieRequestEvent event) {
        try {
            Map<String, Object> requestBody = createGeminiRequest(event.request());
            String rawJson = geminiClient.sendRequest(requestBody, GeminiModel.GEMINI_3_1);
            ExerciseCalorieResponseDTO response = parseCaloriesResponse(rawJson);

            exerciseStoreService.updateCalories(event.exerciseRecordId(), response.getCalories());

            log.info("비동기 칼로리 계산 완료. recordId={}, calories={}", event.exerciseRecordId(), response.getCalories());
        } catch (Exception e) {
            log.error("비동기 칼로리 계산 실패. recordId={}: {}", event.exerciseRecordId(), e.getMessage());
        }
    }

    private Map<String, Object> createGeminiRequest(ExerciseRequestDTO request) {
        String weightStr = request.getWeight() != null ? request.getWeight() + "kg" : "바디웨이트";
        String userPrompt = promptProvider.getExerciseCaloriePrompt().formatted(
                request.getBodyParts(), request.getDuration(), request.getContent(), weightStr);

        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("response_mime_type", "application/json")
        );
    }

    private ExerciseCalorieResponseDTO parseCaloriesResponse(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);
        if (root.has("error")) {
            throw new ExerciseException(ExerciseErrorCode.AI_GENERATION_FAILED_EX);
        }

        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || candidates.isEmpty()) {
            throw new ExerciseException(ExerciseErrorCode.AI_RESPONSE_BLOCKED_EX);
        }

        String jsonContent = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
        return objectMapper.readValue(jsonContent, ExerciseCalorieResponseDTO.class);
    }
}
