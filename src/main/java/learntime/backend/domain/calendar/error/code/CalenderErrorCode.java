package learntime.backend.domain.calendar.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CalenderErrorCode implements BaseErrorCode  {
    CALENDAR_NOT_FOUND(HttpStatus.NOT_FOUND, "CALENDAR-001", "해당 일정을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CalenderErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
