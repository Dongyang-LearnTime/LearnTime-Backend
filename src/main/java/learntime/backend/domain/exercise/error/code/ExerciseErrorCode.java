package learntime.backend.domain.exercise.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExerciseErrorCode implements BaseErrorCode  {

    //Gemini Exercise 관련 에러
    AI_GENERATION_FAILED_EX(HttpStatus.INTERNAL_SERVER_ERROR, "EX-001", "칼로리 계산 중 오류가 발생했습니다."),
    AI_RESPONSE_BLOCKED_EX(HttpStatus.BAD_REQUEST, "EX-002", "AI 답변이 차단되었습니다. 운동과 관련된 내용을 작성해주세요."),

    // 운동 / 신체 데이터 에러
    MEAL_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "EX-003", "식단 데이터를 찾을 수 없습니다."),
    WEIGHT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "EX-004", "신체 데이터를 찾을 수 없습니다."),
    ACCESS_DENIED_WEIGHT(HttpStatus.FORBIDDEN, "EX-005", "해당 신체 데이터에 대한 접근 권한이 없습니다."),
    EXERCISE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "EX-006", "운동 기록을 찾을 수 없습니다."),
    ACCESS_DENIED_EXERCISE(HttpStatus.FORBIDDEN, "EX-007", "해당 운동 기록에 대한 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ExerciseErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
