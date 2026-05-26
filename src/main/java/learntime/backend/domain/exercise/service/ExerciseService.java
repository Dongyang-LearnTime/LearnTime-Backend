package learntime.backend.domain.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.badge.event.ExerciseCompletedEvent;
import learntime.backend.domain.exercise.converter.ExerciseConverter;
import learntime.backend.domain.exercise.dto.request.ExerciseRequestDTO;
import learntime.backend.domain.exercise.dto.response.ExerciseCalorieResponseDTO;
import learntime.backend.domain.exercise.dto.response.ExerciseResponseDTO;
import learntime.backend.domain.exercise.error.code.ExerciseErrorCode;
import learntime.backend.domain.exercise.error.exception.ExerciseException;
import learntime.backend.domain.exercise.model.ExerciseRecord;
import learntime.backend.domain.exercise.repository.ExerciseRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.infra.gemini.GeminiClient;
import learntime.backend.global.infra.youtube.YoutubeClient;
import learntime.backend.global.dto.YoutubeVideoResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseService {
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final YoutubeClient youtubeClient;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final ExercisePromptProvider promptProvider;

    public List<YoutubeVideoResponseDTO> getRecommendedVideos(List<String> bodyParts) {
        if (bodyParts == null || bodyParts.isEmpty()) {
            return youtubeClient.searchVideos("전신 홈 트레이닝");
        }

        String mainPart = String.join(" ", bodyParts) + "운동";
        return youtubeClient.searchVideos(mainPart);
    }

    @Transactional
    public ExerciseResponseDTO saveExercise(Long userId, ExerciseRequestDTO request) {
        User user = userRepository.findById(userId).
                orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        Map<String, Object> requestBody = createGeminiRequest(request);
        try {
            String rawJson = geminiClient.sendRequest(requestBody, GeminiModel.GEMINI_3_1);
            ExerciseCalorieResponseDTO response = parseCaloriesResponse(rawJson);

            ExerciseRecord record = ExerciseConverter.toExerciseRecord(user, request, response);
            ExerciseRecord savedRecord = exerciseRecordRepository.save(record);

            ExerciseResponseDTO result =
                    ExerciseConverter.toExerciseResponseDTO(savedRecord);

            eventPublisher.publishEvent(new ExerciseCompletedEvent(userId, LocalDateTime.now()));
            return result;

        } catch (Exception e) {
            log.error("칼로리 계산 실패: {}", e.getMessage());
            throw new ExerciseException(ExerciseErrorCode.AI_GENERATION_FAILED_EX);
        }
    }

    private Map<String, Object> createGeminiRequest(ExerciseRequestDTO request) {
        String userPrompt = promptProvider.getExerciseCaloriePrompt().formatted(request.getBodyParts(), request.getDuration(), request.getContent());

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
