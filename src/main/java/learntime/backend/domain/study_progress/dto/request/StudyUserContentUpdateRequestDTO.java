package learntime.backend.domain.study_progress.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 정의 계획 내용 수정 요청 DTO")
public record StudyUserContentUpdateRequestDTO(
        @NotBlank(message = "내용은 비어있을 수 없습니다.")
        @Size(max = 150, message = "사용자 진도 내용은 150자 이하여야 합니다.")
        String userContent
) {}
