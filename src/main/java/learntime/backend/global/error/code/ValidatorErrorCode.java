package learntime.backend.global.error.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ValidatorErrorCode implements BaseErrorCode{
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "USER-002", "닉네임에는 특수문자를 사용할 수 없습니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER-003", "이미 사용 중인 닉네임입니다."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "USER-004", "올바른 이메일 형식이 아닙니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-005", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER-006", "비밀번호는 8~32자 사이여야 합니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;

    ValidatorErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
