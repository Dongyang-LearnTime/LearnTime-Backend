package learntime.backend.global.error.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements BaseErrorCode {
    // 할당량 관령 에러
    PROMPT_QUOTA_EXCEEDED(HttpStatus.FORBIDDEN, "QUOTA-001", "프롬프트 사용 가능 횟수를 모두 소진했습니다."),

    // AI 관련 에러
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI-001", "AI 답변 생성 중 오류가 발생했습니다."),
    AI_RESPONSE_BLOCKED(HttpStatus.BAD_REQUEST, "AI-002", "AI 답변이 차단되었습니다. 다시 시도해주세요."),

    // Youtube 관련 에러
    YOUTUBE_API_ERROR(HttpStatus.BAD_GATEWAY, "YOUTUBE-001", "YouTube 응답에 문제가 발생했습니다."),

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
