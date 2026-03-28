package learntime.backend.global.infra.youtube.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeVideoResponseDTO {
    private String videoId;
    private String title;
    private String thumbnailUrl;
}
