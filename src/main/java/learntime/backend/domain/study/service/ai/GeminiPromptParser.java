package learntime.backend.domain.study.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.domain.study.dto.response.QuizQuestionResponseDTO;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

//Gemini API 요청 및 응답 파싱 전담 클래스
@Component
@RequiredArgsConstructor
public class GeminiPromptParser {

    private final ObjectMapper objectMapper;

    private static final Map<String, Object> OCR_SYSTEM_INSTRUCTION = Map.of(
            "parts", List.of(Map.of("text", "너는 OCR(광학 문자 인식) 및 데이터 구조화 전문가야."))
    );

    public Map<String, Object> createRequestBody(
            String userPrompt,
            Map<String, Object> systemInstruction,
            double temperature) {

        return buildBaseRequest(
                List.of(Map.of("text", userPrompt)),
                systemInstruction,
                temperature
        );
    }

    public Map<String, Object> createOcrRequestBody(String userPrompt, String base64Data, String mimeType) {
        return buildBaseRequest(
                List.of(
                        Map.of("text", userPrompt),
                        Map.of("inlineData", Map.of("mimeType", mimeType, "data", base64Data))
                ),
                OCR_SYSTEM_INSTRUCTION,
                0.1
        );
    }

    private Map<String, Object> buildBaseRequest(List<Map<String, Object>> parts, Map<String, Object> systemInstruction, double temperature) {
        return Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "system_instruction", systemInstruction,
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "responseMimeType", "application/json"
                )
        );
    }

    public StudyPlanResponseDTO parseResponse(String rawJson) throws Exception {
        String jsonContent = extractJsonContent(rawJson);
        return objectMapper.readValue(jsonContent, StudyPlanResponseDTO.class);
    }

    public List<TocListResponseDTO> parseOcrResponse(String rawJson) throws Exception {
        String jsonContent = extractJsonContent(rawJson);
        return objectMapper.readValue(jsonContent, new TypeReference<List<TocListResponseDTO>>() {});
    }
    
    public List<QuizQuestionResponseDTO> parseQuizResponse(String rawJson) throws Exception {
        String jsonContent = extractJsonContent(rawJson);
        return objectMapper.readValue(jsonContent, new TypeReference<List<QuizQuestionResponseDTO>>() {});
    }

    private String extractJsonContent(String rawJson) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(rawJson);

        if (root.has("error")) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }

        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_BLOCKED);
        }

        return candidates.get(0).path("content").path("parts").get(0).path("text").asText();
    }
}
