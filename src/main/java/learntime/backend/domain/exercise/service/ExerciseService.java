package learntime.backend.domain.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.exercise.dto.request.ExerciseRequestDTO;
import learntime.backend.domain.exercise.dto.response.ExerciseCalorieResponseDTO;
import learntime.backend.domain.exercise.entity.ExerciseRecord;
import learntime.backend.domain.exercise.repository.ExerciseRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;
import learntime.backend.global.infra.gemini.GeminiClient;
import learntime.backend.global.infra.youtube.YoutubeClient;
import learntime.backend.global.infra.youtube.dto.YoutubeVideoResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<YoutubeVideoResponseDTO> getRecommendedVideos(List<String> bodyParts) {
        if (bodyParts == null || bodyParts.isEmpty()) {
            return youtubeClient.searchVideos("전신 홈 트레이닝");
        }

        String mainPart = bodyParts.getFirst();
        return youtubeClient.searchVideos(mainPart);
    }

    @Transactional
    public ExerciseRecord saveExercise(String email, ExerciseRequestDTO request) {

        // 이메일로 유저 찾기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Map<String, Object> requestBody = createGeminiRequest(request);
        try {
            String rawJson = geminiClient.sendRequest(requestBody);
            ExerciseCalorieResponseDTO response = parseCaloriesResponse(rawJson);

            ExerciseRecord record = ExerciseRecord.builder()
                    .user(user) // 찾은 유저 세팅
                    .bodyParts(request.getBodyParts())
                    .duration(request.getDuration())
                    .content(request.getContent())
                    .calories(response.getCalories())
                    .build();

            return exerciseRecordRepository.save(record);

        } catch (Exception e) {
            log.error("칼로리 계산 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED_EX);
        }
    }

    private Map<String, Object> createGeminiRequest(ExerciseRequestDTO request) {
        String userPrompt = """
                다음의 운동 내역을 바탕으로 소모 칼로리를 계산해줘.
                
                1. 운동 부위: %s
                2. 소요 시간: %d분
                3. 상세 운동 내용: %s
                
                응답은 반드시 아래의 JSON 구조를 지켜서 답해줘.
                {
                  "calories": 숫자
                }
                
                예를 들어 예상 소모 칼로리가 약 400kcal 일 때, 숫자 400만 위의 JSON 구조에 맞춰서 답해주면 돼.
                """.formatted(request.getBodyParts(), request.getDuration(), request.getContent());

        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("response_mime_type", "application/json")
        );
    }

    private ExerciseCalorieResponseDTO parseCaloriesResponse(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);
        if (root.has("error")) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED_EX);
        }

        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_BLOCKED_EX);
        }

        String jsonContent = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
        return objectMapper.readValue(jsonContent, ExerciseCalorieResponseDTO.class);
    }
}