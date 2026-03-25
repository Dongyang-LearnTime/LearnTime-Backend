package learntime.backend.domain.user.dto.request;

import jakarta.validation.constraints.*;

public record SignUpRequestDTO (
    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    @Size(min = 2, max = 25, message = "이름은 2~25자 사이여야 합니다.")
    String userName,

    @NotBlank
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\W_])(?!.*(.)\\1\\1).{8,30}$",
            message = "비밀번호는 8~30자, 영문/숫자/특수문자를 포함하고 같은 문자를 3번 연속 사용할 수 없습니다."
    )
    String password
) { }
