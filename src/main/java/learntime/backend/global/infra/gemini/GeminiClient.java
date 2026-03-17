package learntime.backend.global.infra.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
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
                .retrieve()
                // 에러 발생 시 상세 메시지 출력
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new RuntimeException("Gemini API Client Error: " + response.getStatusText() +
                            " | Body: " + new String(response.getBody().readAllBytes()));
                })
                .body(String.class);
    }
}
