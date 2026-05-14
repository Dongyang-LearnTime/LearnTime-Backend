package learntime.backend.global.error.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements BaseErrorCode {
    // 할당량 관령 에러
    PROMPT_QUOTA_EXCEEDED(
            HttpStatus.FORBIDDEN,
            "QUOTA-001",
            "프롬프트 사용 가능 횟수를 모두 소진했습니다."),

    // AI 관련 에러
    AI_GENERATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "AI-001",
            "AI 답변 생성 중 오류가 발생했습니다."),
    AI_RESPONSE_BLOCKED(
            HttpStatus.BAD_REQUEST,
            "AI-002",
            "AI 답변이 차단되었습니다. 다시 시도해주세요."),

    // Youtube 관련 에러
    YOUTUBE_API_ERROR(
            HttpStatus.BAD_GATEWAY,
            "YOUTUBE-001",
            "YouTube 응답에 문제가 발생했습니다."),

    // S3 관련 에러
    S3_UPLOAD_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "S3-001",
            "S3 파일 업로드에 실패했습니다."),
    S3_DELETE_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "S3-002",
            "S3 파일 삭제에 실패했습니다."),
    S3_URL_INVALID(
            HttpStatus.BAD_REQUEST,
            "S3-003",
            "유효하지 않은 S3 파일 URL입니다."),

    // 신체 데이터 암호화 관련 에러
    ENCRYPTION_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON-500",
            "데이터 암/복호화 중 오류가 발생했습니다."),

    // 기타 공통 에러
    INVALID_INPUT_VALUE
            (HttpStatus.BAD_REQUEST,
                    "COMMON-001",
                    "잘못된 입력값입니다."),
    UTILITY_CLASS_INSTANTIATION(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON500_2",
            "유틸리티 클래스는 인스턴스화 하면 안됩니다."),

    // RequestBody Validation
    INVALID_REQUEST_BODY(
            HttpStatus.BAD_REQUEST,
            "VALIDATOR-001",
            "요청 본문 값이 올바르지 않습니다."
    ),

    // Request Parameter Validation
    INVALID_REQUEST_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "VALIDATOR-002",
            "요청 파라미터 값이 올바르지 않습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
