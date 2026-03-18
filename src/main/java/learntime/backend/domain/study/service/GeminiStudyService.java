package learntime.backend.domain.study.service;

import learntime.backend.domain.study.dto.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.StudyRequestDTO;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;
import learntime.backend.global.infra.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiStudyService {

    private final GeminiClient geminiClient; // 공통 클라이언트 주입
    private final ObjectMapper objectMapper; // JSON 파싱

    public StudyPlanResponseDTO generateSmartStudyPlan(StudyRequestDTO request) {

        // 요청에 대한 시스템 지시문 작성
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", "너는 학습 계획 전문가로서 목차를 검색하고 반드시 JSON 형식으로만 답변해. 다른 말은 하지 마."))
        );

        // 시스템 지시문과 별개로 요청할 내용의 프롬프트
        String userPrompt = """
            1. 구글 검색으로 '%s' 책의 실제 목차와 페이지 수를 확인해줘.
            2. 이를 바탕으로 '%s' 동안의 학습 계획을 세워줘.
            3. 쪽수에 '약'이라는 단어는 제외하고 반드시 p.45-p.67과 같은 형식을 유지해. (단, 검색 결과에서 정확한 페이지를 찾을 수 없는 경우에만 가장 근접한 예상 페이지를 적어줘.)
            4. 난이도는 쉬움, 보통, 어려움으로 구분하고, 내용의 깊이를 기준으로 해.
            5. 응답은 반드시 아래의 JSON 구조를 지켜야 해. 검색 실패 시에도 빈 결과보다는 추측된 계획을 담은 JSON을 반환해:
            
            {
              "daily_plans": [
                {
                  "day": 1,
                  "tasks": [
                    { "chapter_title": "...", "page_range": "...", "difficulty": "..." }
                  ]
                }
              ]
            }
            """.formatted(request.getBookTitle(), request.getPeriod());

        // 시스템 지시문과 프롬프트를 포함하여 요청 바디 구성
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "system_instruction", systemInstruction,
                "tools", List.of(Map.of("google_search", new HashMap<>())),
                "generationConfig", Map.of("temperature", 0.2)
                // 만약 검색 기능을 사용하지 않고자 하면, 위의 google_search를 주석 처리하고, generationConfig 부분에
                // response_mime_type, application/json 옵션을 추가
        );

        String rawJson = "";

        try {
            // rawJson을 원본으로 보고 답변이 잘 생성되었는지 확인
            rawJson = geminiClient.sendRequest(requestBody);

            // Gemini 답변에서 JSON 텍스트만 추출
            // candidates[0] -> content -> parts[0] -> text 의 순서로 변형
            JsonNode root = objectMapper.readTree(rawJson);

            // API 에러 디버깅
            if (root.has("error")) {
                log.error("Gemini API 내부 오류 발생. 메시지: {}, 원본: {}",
                        root.path("error").path("message").asText(""), rawJson);
                throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
            }

            JsonNode candidates = root.path("candidates");

            // candidates 존재 여부 (차단됐는지) 디버깅
            if (candidates.isMissingNode() || candidates.isEmpty()) {
                log.error("AI 응답 차단됨. 원본: {}", rawJson);
                throw new BusinessException(ErrorCode.AI_RESPONSE_BLOCKED);

            }

            JsonNode firstCandidate = candidates.get(0);

            String finishReason = firstCandidate.path("finishReason").asText("");
            if (!"STOP".equals(finishReason) && !finishReason.isEmpty()) {
                log.error("AI 생성 중단됨. 사유: {}, 원본: {}", finishReason, rawJson);
                throw new BusinessException(ErrorCode.AI_RESPONSE_BLOCKED);
            }

            String aiText = firstCandidate.path("content").path("parts").get(0).path("text").asText("");
            String cleanJson = extractJson(aiText);

            // 추출된 JSON 문자열을 DTO로 변환
            return objectMapper.readValue(cleanJson, StudyPlanResponseDTO.class);
        } catch (Exception e) {
            // 위의 기술된 내용 이외의 예외처리 방법
            log.error("학습 계획 생성 중 알 수 없는 오류 발생. 메시지: {}, 원본: {}", e.getMessage(), rawJson);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private String extractJson(String text) {
        if (text == null || text.isBlank()) return "{}";
        if (text.contains("```json")) {
            return text.substring(text.indexOf("```json") + 7, text.lastIndexOf("```")).trim();
        } else if (text.contains("```")) {
            return text.substring(text.indexOf("```") + 3, text.lastIndexOf("```")).trim();
        }
        return text.trim();
    }
}
