package learntime.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import learntime.backend.domain.user.enums.Terms;

@Schema(description = "약관 동의 여부 수정 요청 DTO")
public record UpdateTermsRequestDTO(
        @NotNull(message = "약관 종류는 필수 항목입니다.")
        @Schema(description = "약관 종류 (SERVICE_USE, PRIVACY_POLICY, BODY_DATA_COLLECT)", example = "BODY_DATA_COLLECT")
        Terms terms,

        @NotNull(message = "동의 여부는 필수 항목입니다.")
        @Schema(description = "동의 여부 (true: 동의, false: 철회)", example = "true")
        Boolean agreed
) {
}
