package learntime.backend.domain.message.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "쪽지 전송 요청 DTO")
public record MessageRequestDTO(
        @NotNull(message = "수신자 ID는 필수입니다.")
        @Schema(description = "수신자 고유 ID", example = "2")
        Long receiverId,

        @NotBlank(message = "쪽지 내용은 필수입니다.")
        @Size(max = 1000, message = "쪽지 내용은 최대 1000자까지 작성할 수 있습니다.")
        @Schema(description = "쪽지 본문", example = "안녕하세요! 반갑습니다.")
        String content
) {
}
