package learntime.backend.domain.study.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GeminiStudyRequestDTO {
    private String title;
    private String linkUrl;

    @NotNull
    @Min(value = 7, message = "기간은 최소 7일 이상이어야 합니다.")
    @Max(value = 90, message = "기간은 최대 90일 이하여야 합니다.")
    private int period;
}
