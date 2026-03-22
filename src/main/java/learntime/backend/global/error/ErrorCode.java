package learntime.backend.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Study 관련 에러
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-001", "학습 계획 생성 중 오류가 발생했습니다."),
    AI_RESPONSE_BLOCKED(HttpStatus.BAD_REQUEST, "STUDY-002", "AI 답변이 차단되었습니다. 다른 책으로 시도해주세요."),
    EXTERNAL_CRAWLING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-03", "크롤링 중 오류가 발생했습니다."),
    CRAWLING_DOM_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-04", "대상 사이트의 구조에서 정보를 찾을 수 없습니다"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "STUDY-05", "종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_STUDY_PERIOD(HttpStatus.BAD_REQUEST, "STUDY-06", "학습 기간은 7일 이상 90일 이하만 가능합니다."),

    // 로그인, 인증 관련 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH-001", "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH-002", "비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-003", "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-004", "리프레시 토큰이 만료되었습니다. 다시 로그인하세요."),
    INVALID_JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, "AUTH-005", "잘못된 JWT 서명입니다."),
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-006", "만료된 JWT 토큰입니다."),
    UNSUPPORTED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-007", "지원되지 않는 JWT 토큰입니다."),
    EMPTY_JWT_CLAIM(HttpStatus.UNAUTHORIZED, "AUTH-008", "JWT 토큰이 비어있습니다."),

    // 기타 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-001", "잘못된 입력값입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
