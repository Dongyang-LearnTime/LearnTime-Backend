package learntime.backend.domain.study_forum.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyForumMessageRequestDTO(
        @Schema(description = "토론 메시지 본문", maxLength = 1000)
        @NotBlank @Size(max = 1000) String content
) {}

