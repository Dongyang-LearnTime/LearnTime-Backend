package learntime.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "회원 탈퇴 요청 DTO")
public record DeleteAccountRequestDTO(
        @NotBlank(message = "탈퇴 확인 문구를 입력해주세요.")
        @Pattern(regexp = "^회원 탈퇴$", message = "정확히 '회원 탈퇴'라고 입력해주세요.")
        @Schema(description = "탈퇴 확인 문구 ('회원 탈퇴' 입력 필수)", example = "회원 탈퇴")
        String confirmation,

        @Schema(description = "소셜 로그인 유저의 경우 연동 해제를 위한 액세스 토큰 (로컬 유저는 null)", example = "ya29.a0AfB_by...")
        String socialToken
) {
}
