package learntime.backend.domain.study.service.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
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

    // 공부 일정 생성 INSTRUCTION
    private static final Map<String, Object> STUDY_SYSTEM_INSTRUCTION = Map.of(
            "parts", List.of(Map.of("text", "너는 도서의 커리큘럼을 짜는 학습 계획 전문가야."))
    );

    // 사진 분석 INSTRUCTION
    private static final Map<String, Object> OCR_SYSTEM_INSTRUCTION = Map.of(
            "parts", List.of(Map.of("text", "너는 OCR(광학 문자 인식) 및 데이터 구조화 전문가야."))
    );

    // 제미나이 생성 (Study Plan)
    public Map<String, Object> createRequestBody(String userPrompt) {
        return buildBaseRequest(
                List.of(Map.of("text", userPrompt)),
                STUDY_SYSTEM_INSTRUCTION,
                0.2
        );
    }

    // 제미나이 생성 (OCR)
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

    // 제미나이 api 요청 body
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

    // 응답 StudyPlanResponseDTO 으로 파싱
    public StudyPlanResponseDTO parseResponse(String rawJson) throws Exception {
        String jsonContent = extractJsonContent(rawJson); // 공통 추출 로직 호출 후 단일 객체로 역직렬화
        return objectMapper.readValue(jsonContent, StudyPlanResponseDTO.class);
    }

    // 응답 파싱 (OCR List)
    public List<TocListResponseDTO> parseOcrResponse(String rawJson) throws Exception {
        // 공통 추출 로직 호출 후 List(컬렉션) 객체로 역직렬화
        String jsonContent = extractJsonContent(rawJson);
        return objectMapper.readValue(jsonContent, new TypeReference<List<TocListResponseDTO>>() {});
    }

    /**
     * Gemini JSON 검증 및 실제 텍스트 Content 추출
     * 예외 처리 로직(에러 노드 확인, 빈 응답 확인)을 한 곳으로 응집
     */
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
