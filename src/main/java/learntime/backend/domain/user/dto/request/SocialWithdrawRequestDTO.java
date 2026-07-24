package learntime.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 회원 탈퇴 요청 DTO")
public record SocialWithdrawRequestDTO(
        @NotBlank(message = "소셜 토큰은 필수입니다.")
        @Schema(description = "프론트엔드에서 발급받은 소셜 토큰 (구글 연결 끊기 용도)", example = "ya29.a0AfB_by...")
        String token
) {
}
