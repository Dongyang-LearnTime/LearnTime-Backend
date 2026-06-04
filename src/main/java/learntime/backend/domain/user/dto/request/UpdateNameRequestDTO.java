package learntime.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "이름 수정 요청 DTO")
public record UpdateNameRequestDTO(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 25, message = "이름은 2~25자 사이여야 합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣_\\\\-\\\\s]+$", message = "이름에 특수문자(JS 태그, SQL 등)는 사용할 수 없습니다.")
        String name
) {}
