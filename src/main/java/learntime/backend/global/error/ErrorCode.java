package learntime.backend.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    //Gemini Study 관련 에러
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-001", "학습 계획 생성 중 오류가 발생했습니다."),
    AI_RESPONSE_BLOCKED(HttpStatus.BAD_REQUEST, "STUDY-002", "AI 답변이 차단되었습니다. 다른 책으로 시도해주세요."),

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
