package learntime.backend.global.infra.gemini;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import learntime.backend.global.common.GeminiModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 300,
                    multiplier = 2
            )
    )
    public String sendRequest(
            Map<String, Object> requestBody,
            GeminiModel model
    ) {
        long startTime = System.nanoTime();

        return restClient.post()
                .uri(model.getEndpoint() + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange((request, response) -> {
                    // InputStream 기반 응답 처리
                    InputStream bodyStream = response.getBody();

                    // 응답 전체를 byte[]로 읽은 이후 UTF-8 String 변환
                    byte[] responseBytes = bodyStream.readAllBytes();

                    // API 전체 latency 계산
                    long durationMs =
                            (System.nanoTime() - startTime) / 1_000_000;

                    // 4xx / 5xx 응답 처리
                    if (response.getStatusCode().isError()) {
                        // 에러 상황에서만 String 생성
                        String errorBody = new String(
                                responseBytes,
                                StandardCharsets.UTF_8
                        );

                        throw new RuntimeException(
                                "Gemini API Error [%s] | Body: %s"
                                        .formatted(
                                                response.getStatusCode(),
                                                errorBody
                                        )
                        );
                    }

                    // 로그 찍음
                    logTokenUsage(responseBytes, durationMs);

                    return new String(
                            responseBytes,
                            StandardCharsets.UTF_8
                    );
                });
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 300, multiplier = 2)
    )
    public String uploadFile(byte[] fileBytes, String mimeType, String displayName) {
        long startTime = System.nanoTime();
        
        return restClient.post()
                .uri(GeminiModel.UPLOAD_ENDPOINT + "?key=" + apiKey)
                .contentType(MediaType.parseMediaType(mimeType))
                .header("X-Goog-Upload-Protocol", "raw")
                .header("X-Goog-Upload-File-Data", displayName)
                .body(fileBytes)
                .exchange((request, response) -> {
                    InputStream bodyStream = response.getBody();
                    byte[] responseBytes = bodyStream.readAllBytes();
                    
                    if (response.getStatusCode().isError()) {
                        String errorBody = new String(responseBytes, StandardCharsets.UTF_8);
                        throw new RuntimeException("Gemini Upload Error [%s] | Body: %s".formatted(response.getStatusCode(), errorBody));
                    }
                    
                    String jsonResponse = new String(responseBytes, StandardCharsets.UTF_8);
                    
                    long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                    log.info("[Gemini Upload API] {}ms | size: {} bytes", durationMs, fileBytes.length);
                    
                    try {
                        JsonNode root = objectMapper.readTree(jsonResponse);
                        return root.path("file").path("uri").asText();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse upload response", e);
                    }
                });
    }


    private void logTokenUsage(byte[] responseBytes, long durationMs) {
        int promptTokens = 0;
        int candidateTokens = 0;
        int cachedTokens = 0;
        int thoughtsTokens = 0;
        int totalTokens = 0;

        try (
                JsonParser parser =
                        JSON_FACTORY.createParser(responseBytes)

        ) {
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token != JsonToken.FIELD_NAME) {
                    continue;
                }

                // 현재 필드명
                String fieldName = parser.currentName();

                parser.nextToken();

                // 필요한 usage field만 추출
                switch (fieldName) {
                    case "promptTokenCount" ->
                            promptTokens = parser.getIntValue();
                    case "candidatesTokenCount" ->
                            candidateTokens = parser.getIntValue();
                    case "cachedContentTokenCount" ->
                            cachedTokens = parser.getIntValue();
                    case "thoughtsTokenCount" ->
                            thoughtsTokens = parser.getIntValue();
                    case "totalTokenCount" ->
                            totalTokens = parser.getIntValue();
                }
            }

            log.info(
                    "[Gemini API] {}ms | Prompt={} (Cached={}) | Candidate={} (Thoughts={}) | Total={}",
                    durationMs,
                    promptTokens,
                    cachedTokens,
                    candidateTokens,
                    thoughtsTokens,
                    totalTokens
            );

        } catch (Exception e) {
            log.warn(
                    "Failed to parse Gemini usage metadata",
                    e
            );
        }
    }
}