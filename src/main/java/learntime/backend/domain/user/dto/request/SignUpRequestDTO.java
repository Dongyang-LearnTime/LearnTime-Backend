package learntime.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import learntime.backend.domain.user.enums.Terms;

import java.util.Map;

@Schema(description = "회원가입 요청 정보를 담은 DTO")
public record SignUpRequestDTO (

    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    @Size(min = 2, max = 25, message = "이름은 2~25자 사이여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣_\\\\-\\\\s]+$", message = "이름에 특수문자(JS 태그, SQL 등)는 사용할 수 없습니다.")
    String userName,

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @Schema(
            description = "비밀번호 (8~30자, 영문/숫자/특수문자 포함, 동일 문자 3회 연속 불가)",
            example = "Abc123!@#"
    )
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\W_])(?!.*(.)\\1\\1).{8,30}$",
            message = "비밀번호는 8~30자, 영문/숫자/특수문자를 포함하고 같은 문자를 3번 연속 사용할 수 없습니다."
    )
    String password,

    @Schema(
            description = "약관 동의 여부 Map (key: Terms enum, value: 동의 여부)",
            example = """
    {
      "SERVICE_USE": true,
      "PRIVACY_POLICY": true,
      "BODY_DATA_COLLECT": false
    }
    """
    )
    Map<Terms, Boolean> termsAgreements

) { }
