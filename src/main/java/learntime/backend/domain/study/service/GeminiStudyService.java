package learntime.backend.domain.study.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;
import learntime.backend.global.infra.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiStudyService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    // 시스템 지시문
    private static final Map<String, Object> SYSTEM_INSTRUCTION = Map.of(
            "parts", List.of(Map.of("text", "너는 도서의 커리큘럼을 짜는 학습 계획 전문가야."))
    );


    public StudyPlanResponseDTO generateSmartStudyPlan(GeminiStudyRequestDTO request) {

        // 크롤링 로직 직접 실행
        String bookToc = extractToc(request.getLinkUrl());

        // 프롬프트 및 요청 바디 생성
        Map<String, Object> requestBody = createRequestBody(request.getPeriod(), request.getTitle(), bookToc);

        try {
            String rawJson = geminiClient.sendRequest(requestBody);
            return parseGeminiResponse(rawJson);
        } catch (Exception e) {
            log.error("AI 학습 계획 생성 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    // 목차 크롤링
    private String extractToc(String linkUrl) {
        try {
            Document doc = Jsoup.connect(linkUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(5000)
                    .get();

            Element textAreaTag = doc.selectFirst("#infoset_toc .txtContentText");
            if (textAreaTag == null) {
                log.warn("목차 요소를 찾을 수 없습니다. URL: {}", linkUrl);
                return "목차 정보 없음";
            }

            String htmlWithBr = textAreaTag.html().replaceAll("(?i)<br\\s*/?>", "\n");
            return Jsoup.parse(htmlWithBr).text();

        } catch (IOException e) {
            log.error("크롤링 중 I/O 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    private Map<String, Object> createRequestBody(int period, String bookTitle, String bookToc) {
        String userPrompt = """
                다음 책의 목차를 바탕으로 %d일 동안의 학습 계획을 세워줘(목차에서 정답, 해설, 서문같은 내용은 제외)
                
                책 제목: %s
                책 목차: 
                %s
                
                요구사항:
                1. 주어진 기간에 맞게 하루 단위로 분량을 분배해.
                2. 난이도는 '쉬움', '보통', '어려움'으로 표기해.
                3. 반드시 아래 JSON 스키마 구조를 엄격히 지켜서 응답해.
                
                {
                  "daily_plans": [
                    {
                      "day": 1,
                      "tasks": [
                        { "chapter_title": "...", "difficulty": "..." }
                      ]
                    }
                  ]
                }
                """.formatted(period, bookTitle, bookToc);

        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "system_instruction", SYSTEM_INSTRUCTION,
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );
    }

    private StudyPlanResponseDTO parseGeminiResponse(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);

        if (root.has("error")) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }

        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_BLOCKED);
        }

        String jsonContent = candidates.get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text").asText();

        return objectMapper.readValue(jsonContent, StudyPlanResponseDTO.class);
    }
}