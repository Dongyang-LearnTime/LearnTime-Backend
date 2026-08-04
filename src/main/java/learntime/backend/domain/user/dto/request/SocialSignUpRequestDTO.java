package learntime.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Terms;

import java.util.Map;

@Schema(description = "소셜 회원가입 요청 DTO")
public record SocialSignUpRequestDTO(
        @NotNull(message = "소셜 제공자(provider)는 필수입니다. (GOOGLE, KAKAO 등)")
        @Schema(description = "소셜 플랫폼 (GOOGLE, KAKAO)", example = "GOOGLE")
        AuthProvider provider,

        @NotBlank(message = "소셜 토큰은 필수입니다.")
        @Schema(description = "프론트엔드에서 발급받은 소셜 토큰 (access_token 또는 id_token)", example = "ya29.a0AfB_by...")
        String token,

        @NotBlank(message = "이름을 입력해주세요.")
        @Schema(description = "사용자 이름(닉네임)", example = "소셜가입자1")
        String userName,

        @NotNull(message = "약관 동의 정보는 필수입니다.")
        @Schema(description = "약관 동의 정보 (key: 약관 Enum, value: 동의 여부)",
                example = "{\"SERVICE_USE\": true, \"PRIVACY_POLICY\": true, \"BODY_DATA_COLLECT\": false}")
        Map<Terms, Boolean> termsAgreements
) {
}
