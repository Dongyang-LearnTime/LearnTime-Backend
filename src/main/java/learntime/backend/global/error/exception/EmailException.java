package learntime.backend.global.error.exception;

import learntime.backend.global.error.code.EmailErrorCode;

public class EmailException extends BaseException {
    public EmailException(EmailErrorCode emailErrorCode) {
        super(emailErrorCode);
    }
}
