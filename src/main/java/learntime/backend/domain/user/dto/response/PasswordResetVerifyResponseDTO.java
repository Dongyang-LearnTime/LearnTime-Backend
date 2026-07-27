package learntime.backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비밀번호 재설정 이메일 인증 확인 응답 DTO")
public record PasswordResetVerifyResponseDTO(

        @Schema(description = "비밀번호 재설정 시 사용할 일회성 토큰")
        String resetToken

) {
}
