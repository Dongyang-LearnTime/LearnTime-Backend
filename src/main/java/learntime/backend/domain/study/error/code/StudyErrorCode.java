package learntime.backend.domain.study.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum StudyErrorCode implements BaseErrorCode {
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "STUDY-001", "종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_STUDY_PERIOD(HttpStatus.BAD_REQUEST, "STUDY-002", "학습 기간은 14일 이상 90일 이하만 가능합니다."),
    STUDY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-003", "학습 계획을 저장하는 도중 데이터베이스 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    StudyErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
