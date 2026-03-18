package learntime.backend.domain.study.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GeminiStudyRequestDTO {
    private String title;
    private String linkUrl;
    private int period; // 1일 단위
}
