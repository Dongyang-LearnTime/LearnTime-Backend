package learntime.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이름 수정 요청 DTO")
public record UpdateNameRequestDTO(
        @NotBlank(message = "이름은 필수입니다.")
        String name
) {}
