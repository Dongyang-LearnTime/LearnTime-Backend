package learntime.backend.global.infra.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.global.common.GeminiModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String sendRequest(Map<String, Object> requestBody, GeminiModel model) {
        long startTime = System.nanoTime(); // 시간 측정 시작

        return restClient.post()
                .uri(model.getEndpoint() + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange((request, response) -> {
                    // 응답 바디를 무조건 바이트 배열
                    byte[] bodyBytes = response.getBody().readAllBytes();
                    String bodyString = new String(bodyBytes, StandardCharsets.UTF_8);

                    long durationMs = (System.nanoTime() - startTime) / 1_000_000; // 측정 종료

                    // 에러 발생 시 처리 (4xx, 5xx)
                    if (response.getStatusCode().isError()) {
                        throw new RuntimeException("Gemini API Error [" + response.getStatusCode() + "] | Body: " + bodyString);
                    }

                    logTokenUsage(bodyString, durationMs); // 토큰량, 시간 로그로 찍음

                    // 정상 응답 반환
                    return bodyString;
                });
    }

    private void logTokenUsage(String responseBody, long durationMs) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usage = root.path("usageMetadata");

            if (!usage.isMissingNode()) {
                int promptTokens = usage.path("promptTokenCount").asInt(0);
                int candidateTokens = usage.path("candidatesTokenCount").asInt(0);

                int cachedTokens = usage.path("cachedContentTokenCount").asInt(0);
                int thoughtsTokens = usage.path("thoughtsTokenCount").asInt(0);

                int totalTokens = usage.path("totalTokenCount").asInt(0);

                log.info("[Gemini API] Latency: {}ms | Tokens - Prompt: {} (Cached: {}), Candidate: {} (Thoughts: {}), Total: {}",
                        durationMs, promptTokens, cachedTokens, candidateTokens, thoughtsTokens, totalTokens);
            }
        } catch (Exception e) {
            log.warn("Failed to parse token usage metadata", e);
        }
    }

}
