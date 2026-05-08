package learntime.backend.domain.notes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "공부 필기 수정 요청 DTO")
@Builder
public record StudyNotesUpdateRequestDTO(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        @Schema(description = "수정할 필기 제목")
        String title,

        @NotBlank(message = "본문을 입력해주세요.")
        @Size(max = 100000, message = "본문 내용이 너무 깁니다.")
        @Schema(description = "수정할 필기 본문")
        String content
) {
}
