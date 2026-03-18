package learntime.backend.global.infra.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class GeminiClient {

    // gemini-2.5-flash
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestClient restClient;

    public GeminiClient(RestClient restClient) {
        this.restClient = restClient;
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

                    // 3. 정상 응답 반환
                    return bodyString;
                });
    }
}
