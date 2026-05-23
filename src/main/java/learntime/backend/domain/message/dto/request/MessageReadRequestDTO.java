package learntime.backend.domain.message.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "쪽지 읽음 처리 요청 DTO")
public record MessageReadRequestDTO(
        @NotEmpty(message = "읽음 처리할 쪽지 ID 목록은 필수입니다.")
        @Schema(description = "읽음 처리할 쪽지 ID 목록", example = "[1, 2, 3]")
        List<Long> messageIds
) {
}
