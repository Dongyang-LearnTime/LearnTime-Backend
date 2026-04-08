package learntime.backend.domain.study.service;

import learntime.backend.global.infra.gemini.GeminiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TocExtractionService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    /**
     * 목차 이미지를 분석하여 JSON 문자열로 반환합니다.
     * 시간 복잡도: O(N) (N은 이미지의 픽셀/바이트 수, Base64 인코딩 시 소요)
     */
    public String extractTocAsJson(MultipartFile imageFile) {
        try {
            // 1. 이미지를 Base64로 인코딩 (메모리 관리에 유의)
            String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
            String mimeType = imageFile.getContentType() != null ? imageFile.getContentType() : "image/jpeg";

            // 2. Gemini API 요청 본문(Payload) 구성
            Map<String, Object> requestBody = createGeminiRequest(base64Image, mimeType);

            // 3. GeminiClient를 통해 API 호출
            String rawResponse = geminiClient.sendRequest(requestBody);

            // 4. 응답 파싱 및 순수 JSON 텍스트 추출
            return parseGeminiResponse(rawResponse);

        } catch (IOException e) {
            log.error("이미지 처리 중 오류 발생", e);
            throw new RuntimeException("이미지 파일 읽기 실패");
        }
    }

    private Map<String, Object> createGeminiRequest(String base64Data, String mimeType) {
        // 프롬프트: 모델에게 명확한 역할과 JSON 스키마를 지시
        String prompt = "너는 OCR(광학 문자 인식) 및 데이터 구조화 전문가야.\n" +
                "1. 첨부된 이미지에서 '목차' 부분을 찾아 모든 챕터 번호, 제목, 페이지 번호를 추출해.\n" +
                "2. 제목이 여러 줄일 경우 하나로 합치고, 불필요한 점선(...)은 무시해.\n" +
                "3. 반드시 아래 JSON 배열 형식만 반환하고, 부연 설명은 하지 마.\n" +
                "4. 텍스트가 흐릿하더라도 주변 맥락을 통해 최대한 정확한 단어를 유추해.\n" +
                "5. 만약 이미지에서 페이지 번호를 찾을 수 없다면 'page' 값을 null로 설정해. " +
                "절대 임의의 숫자를 지어내지 마." +
                "형식: [{\"chapter\": \"string\", \"title\": \"string\", \"page\": number}]";

        // Multimodal Part 구성
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> inlineDataPart = Map.of(
                "inlineData", Map.of(
                        "mimeType", mimeType,
                        "data", base64Data
                )
        );

        // API 스펙에 맞춘 계층 구조 생성
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(textPart, inlineDataPart))
                ),
                // JSON 응답을 강제하기 위한 설정
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.1 // 창의성보다는 정확성이 중요하므로 낮게 설정
                )
        );
    }

    private String parseGeminiResponse(String responseBody) throws JsonProcessingException {
        // Gemini API의 응답 JSON 구조에서 실제 생성된 텍스트(JSON)만 추출
        var rootNode = objectMapper.readTree(responseBody);
        var textNode = rootNode.path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text");

        if (textNode.isMissingNode()) {
            throw new RuntimeException("제미나이 응답에서 텍스트를 찾을 수 없습니다.");
        }
        return textNode.asText();
    }
}