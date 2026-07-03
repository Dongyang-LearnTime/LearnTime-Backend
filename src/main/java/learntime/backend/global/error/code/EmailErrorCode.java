package learntime.backend.global.error.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum EmailErrorCode implements BaseErrorCode {
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL-001", "이메일 전송에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    EmailErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
