package learntime.backend.domain.study.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum StudyErrorCode implements BaseErrorCode {
    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-001", "공부 진도를 찾을 수 없습니다."),
    STUDY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-002", "공부 맴버를 찾을 수 없습니다."),
    STUDY_DAILY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-003", "공부 일일 진도를 찾을 수 없습니다."),

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "STUDY-004", "종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_STUDY_PERIOD(HttpStatus.BAD_REQUEST, "STUDY-005", "학습 기간은 14일 이상 90일 이하만 가능합니다."),
    STUDY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-006", "학습 계획을 저장하는 도중 데이터베이스 오류가 발생했습니다."),
    STUDY_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-007", "공부 필기를 찾을 수 없습니다."),
    STUDY_UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "STUDY-008", "해당 공부 진도/필기에 대한 접근 권한이 없습니다."),
    QUIZ_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-009", "퀴즈 문제를 찾을 수 없습니다."),
    QUIZ_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-010", "퀴즈 풀이 이력을 찾을 수 없습니다."),
    STUDY_DAILY_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "STUDY-011", "이미 완료된 공부 일일 일정에는 사용자 내용을 추가할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    StudyErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
