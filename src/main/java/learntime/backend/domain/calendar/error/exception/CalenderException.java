package learntime.backend.domain.calendar.error.exception;

import learntime.backend.domain.calendar.error.code.CalenderErrorCode;
import learntime.backend.global.error.exception.BaseException;

public class CalenderException extends BaseException {
    public CalenderException(CalenderErrorCode calenderErrorCode) {
        super(calenderErrorCode);
    }
}
