package learntime.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "비밀번호 최종 재설정 요청 DTO")
public record PasswordResetConfirmRequestDTO(

        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(description = "인증 성공 후 발급받은 일회성 비밀번호 재설정 토큰")
        @NotBlank(message = "재설정 토큰은 필수입니다.")
        String resetToken,

        @Schema(
                description = "새 비밀번호 (8~30자, 영문/숫자/특수문자 포함, 동일 문자 3회 연속 불가)",
                example = "NewPassword123!@"
        )
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\W_])(?!.*(.)\\1\\1).{8,30}$",
                message = "비밀번호는 8~30자, 영문/숫자/특수문자를 포함하고 같은 문자를 3번 연속 사용할 수 없습니다."
        )
        String newPassword

) {
}
