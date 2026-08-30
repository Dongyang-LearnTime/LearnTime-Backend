package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "스터디 공개 여부 변경 요청 DTO")
public record UpdateStudyVisibilityRequestDTO(
        @NotNull(message = "공개 여부는 필수입니다.")
        @Schema(description = "스터디 공개 여부 (true: 공개, false: 비공개)", example = "true")
        Boolean isPublic
) {
}
