package learntime.backend.global.error.exception;

import learntime.backend.global.error.code.ErrorCode;

public class BusinessException extends BaseException {
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
