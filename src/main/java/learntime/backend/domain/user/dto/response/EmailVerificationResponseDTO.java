package learntime.backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증 성공 응답 DTO")
public record EmailVerificationResponseDTO(

        @Schema(description = "회원가입 요청에 포함할 일회성 이메일 인증 토큰")
        String verificationToken

) {
}
