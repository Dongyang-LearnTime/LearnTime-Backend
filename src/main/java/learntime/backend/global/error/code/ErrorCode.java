package learntime.backend.global.error.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements BaseErrorCode {
    // 할당량 관령 에러
    PROMPT_QUOTA_EXCEEDED(HttpStatus.FORBIDDEN, "QUOTA-001", "프롬프트 사용 가능 횟수를 모두 소진했습니다."),

    // Study 관련 에러
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-001", "학습 계획 생성 중 오류가 발생했습니다."),
    AI_RESPONSE_BLOCKED(HttpStatus.BAD_REQUEST, "STUDY-002", "AI 답변이 차단되었습니다. 다른 책으로 시도해주세요."),
    EXTERNAL_CRAWLING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-03", "크롤링 중 오류가 발생했습니다."),
    CRAWLING_DOM_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-04", "대상 사이트의 구조에서 정보를 찾을 수 없습니다"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "STUDY-05", "종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_STUDY_PERIOD(HttpStatus.BAD_REQUEST, "STUDY-06", "학습 기간은 14일 이상 90일 이하만 가능합니다."),
    STUDY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-007", "학습 계획을 저장하는 도중 데이터베이스 오류가 발생했습니다."),

    //Gemini Exercise 관련 에러
    AI_GENERATION_FAILED_EX(HttpStatus.INTERNAL_SERVER_ERROR, "EX-001", "칼로리 계산 중 오류가 발생했습니다."),
    AI_RESPONSE_BLOCKED_EX(HttpStatus.BAD_REQUEST, "EX-002", "AI 답변이 차단되었습니다. 운동과 관련된 내용을 작성해주세요."),

    // Youtube 관련 에러
    YOUTUBE_API_ERROR(HttpStatus.BAD_GATEWAY, "YOUTUBE-001", "YouTube 응답에 문제가 발생했습니다."),

    // 캘린더 관련 에러
    CALENDAR_NOT_FOUND(HttpStatus.NOT_FOUND, "CALENDAR-001", "해당 일정을 찾을 수 없습니다."),

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
