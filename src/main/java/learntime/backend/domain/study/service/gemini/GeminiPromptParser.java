package learntime.backend.domain.study.service.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiPromptParser {

    private final ObjectMapper objectMapper;

    private static final Map<String, Object> SYSTEM_INSTRUCTION = Map.of(
            "parts", List.of(Map.of("text", "너는 도서의 커리큘럼을 짜는 학습 계획 전문가야."))
    );

    // 제미나이 생성
    public Map<String, Object> createRequestBody(String userPrompt) {
        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "system_instruction", SYSTEM_INSTRUCTION,
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );
    }

    // 응답 StudyPlanResponseDTO 으로 파싱
    public StudyPlanResponseDTO parseResponse(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);

        if (root.has("error")) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }

        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_BLOCKED);
        }

        String jsonContent = candidates.get(0)
                .path("content").path("parts").get(0).path("text").asText();

        return objectMapper.readValue(jsonContent, StudyPlanResponseDTO.class);
    }
}
