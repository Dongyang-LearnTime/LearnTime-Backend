package learntime.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "카카오 로그인 연동 해제 및 일반 계정 전환 요청 DTO")
public record UnlinkKakaoRequestDTO(

    @Schema(description = "카카오 Access Token", example = "access_token_sample_123")
    @NotBlank(message = "카카오 토큰은 필수 입력 항목입니다.")
    String kakaoToken,

    @Schema(
        description = "전환 후 사용할 신규 비밀번호 (8~30자, 영문/숫자/특수문자 포함, 동일 문자 3회 연속 불가)",
        example = "NewPassword123!"
    )
    @NotBlank(message = "새로운 비밀번호는 필수 입력 항목입니다.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\W_])(?!.*(.)\\1\\1).{8,30}$",
        message = "비밀번호는 8~30자, 영문/숫자/특수문자를 포함하고 같은 문자를 3번 연속 사용할 수 없습니다."
    )
    String newPassword
) { }
