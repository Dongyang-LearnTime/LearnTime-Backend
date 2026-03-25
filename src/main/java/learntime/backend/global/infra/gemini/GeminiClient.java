package learntime.backend.global.infra.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {

    // 3.0 버전
//    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent";

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public String sendRequest(Map<String, Object> requestBody) {
        return restClient.post()
                .uri(GEMINI_URL + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange((request, response) -> {
                    // 1. 응답 바디를 무조건 바이트 배열
                    byte[] bodyBytes = response.getBody().readAllBytes();
                    String bodyString = new String(bodyBytes, StandardCharsets.UTF_8);

                    // 2. 에러 발생 시 처리 (4xx, 5xx)
                    if (response.getStatusCode().isError()) {
                        throw new RuntimeException("Gemini API Error [" + response.getStatusCode() + "] | Body: " + bodyString);
                    }

                    logTokenUsage(bodyString);

                    // 3. 정상 응답 반환
                    return bodyString;
                });
    }

    private void logTokenUsage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usage = root.path("usageMetadata");

            if (!usage.isMissingNode()) {
                int promptTokens = usage.path("promptTokenCount").asInt(0);
                int candidateTokens = usage.path("candidatesTokenCount").asInt(0);

                int cachedTokens = usage.path("cachedContentTokenCount").asInt(0);
                int thoughtsTokens = usage.path("thoughtsTokenCount").asInt(0);

                int totalTokens = usage.path("totalTokenCount").asInt(0);

                log.info("[Gemini Token Usage] Prompt: {} (Cached: {}), Candidate: {} (Thoughts: {}), Total: {}",
                        promptTokens, cachedTokens, candidateTokens, thoughtsTokens, totalTokens);
            }
        } catch (Exception e) {
            log.warn("Failed to parse token usage metadata", e);
        }
    }

}
