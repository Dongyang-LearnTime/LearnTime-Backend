package learntime.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import learntime.backend.domain.user.enums.AuthProvider;

@Schema(description = "소셜 로그인 요청 DTO")
public record SocialLoginRequestDTO(
        @NotNull(message = "소셜 제공자(provider)는 필수입니다. (GOOGLE, KAKAO 등)")
        @Schema(description = "소셜 플랫폼 (GOOGLE, KAKAO)", example = "GOOGLE")
        AuthProvider provider,

        @NotBlank(message = "소셜 토큰은 필수입니다.")
        @Schema(description = "프론트엔드에서 발급받은 소셜 토큰 (access_token 또는 id_token)", example = "ya29.a0AfB_by...")
        String token
) {
}
