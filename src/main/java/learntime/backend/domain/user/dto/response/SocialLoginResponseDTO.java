package learntime.backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소셜 로그인 응답 DTO")
public record SocialLoginResponseDTO(
        @Schema(description = "가입 여부 (true: 가입완료 및 로그인 성공, false: 미가입 상태로 회원가입 폼으로 이동 필요)", example = "true")
        boolean isRegistered,
        
        @Schema(description = "발급된 Access Token (isRegistered가 false일 경우 null)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        
        @Schema(description = "토큰 타입 (일반적으로 Bearer)", example = "Bearer")
        String tokenType
) {
    public static SocialLoginResponseDTO notRegistered() {
        return new SocialLoginResponseDTO(false, null, null);
    }
    
    public static SocialLoginResponseDTO success(String accessToken) {
        return new SocialLoginResponseDTO(true, accessToken, "Bearer");
    }
}
