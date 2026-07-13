package learntime.backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "관리자 사용자 포인트 지급 요청 DTO")
public record AdminPointAwardRequest(
        @NotNull(message = "지급할 포인트는 필수입니다.")
        @Min(value = 1, message = "지급할 포인트는 최소 1포인트 이상이어야 합니다.")
        @Schema(description = "지급 포인트 수량", example = "100")
        Integer amount,

        @NotBlank(message = "지급 사유는 필수입니다.")
        @Schema(description = "지급 사유", example = "이벤트 참여 보상")
        String description
) {}
