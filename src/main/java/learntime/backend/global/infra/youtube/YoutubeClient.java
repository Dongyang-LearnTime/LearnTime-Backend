package learntime.backend.global.infra.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.global.infra.youtube.dto.YoutubeVideoResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class YoutubeClient {

    @Value("${youtube.key}")
    private String apiKey;

    private final RestClient restClient; // Gemini API 통신 설정값
    private final ObjectMapper objectMapper;

    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";

    public List<YoutubeVideoResponseDTO> searchVideos(String query) {
        try {
            String rawJson = restClient.get()
                    .uri(YOUTUBE_SEARCH_URL, uriBuilder -> uriBuilder
                            .queryParam("part", "snippet")
                            .queryParam("q", query + "운동")
                            .queryParam("maxResults", 3)
                            .queryParam("type", "video")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(String.class);
            return parseYoutubeResponse(rawJson);
        } catch (Exception e) {
            log.error("유튜브 검색 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<YoutubeVideoResponseDTO> parseYoutubeResponse(String rawJson) throws Exception{
        JsonNode root = objectMapper.readTree(rawJson);
        List<YoutubeVideoResponseDTO> videos = new ArrayList<>();

        for (JsonNode item : root.path("items")) {
            String videoId = item.path("id").path("videoId").asText();
            String title = item.path("snippet").path("title").asText();
            String thumbnailUrl = item.path("snippet").path("thumbnails").path("medium").path("url").asText();
            videos.add(new YoutubeVideoResponseDTO(videoId, title, thumbnailUrl));
        }
        return videos;
    }
}
