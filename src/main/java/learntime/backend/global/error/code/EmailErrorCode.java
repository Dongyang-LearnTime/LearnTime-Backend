package learntime.backend.global.error.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum EmailErrorCode implements BaseErrorCode {
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL-001", "이메일 전송에 실패했습니다."),
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "EMAIL-002", "이메일 인증 요청을 찾을 수 없습니다."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "EMAIL-003", "이메일 인증 코드가 만료되었습니다."),
    EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "EMAIL-004", "이메일 인증 코드가 일치하지 않습니다."),
    EMAIL_VERIFICATION_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "EMAIL-005", "이메일 인증 시도 횟수를 초과했습니다."),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "EMAIL-006", "이메일 인증이 필요합니다."),
    EMAIL_VERIFICATION_INVALID(HttpStatus.BAD_REQUEST, "EMAIL-007", "유효하지 않은 이메일 인증입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    EmailErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
