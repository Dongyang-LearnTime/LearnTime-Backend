package learntime.backend.global.error.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH-001", "사용자를 찾을 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-002", "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-003", "리프레시 토큰이 만료되었습니다. 다시 로그인하세요."),
    INVALID_JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, "AUTH-004", "잘못된 JWT 서명입니다."),
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-005", "만료된 JWT 토큰입니다."),
    UNSUPPORTED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-006", "지원되지 않는 JWT 토큰입니다."),
    EMPTY_JWT_CLAIM(HttpStatus.UNAUTHORIZED, "AUTH-007", "JWT 토큰이 비어있습니다."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "AUTH-008", "필수 약관에 동의해야 합니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "AUTH-009", "해당 자원에 대한 접근 권한이 없습니다."),
    USER_NAME_DUPLICATED(HttpStatus.CONFLICT, "AUTH-010", "이미 사용 중인 이름입니다."),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "AUTH-011", "비밀번호가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
