package learntime.backend.global.error.exception;

import learntime.backend.global.error.code.ErrorCode;

public class EncryptionException extends BaseException {
    public EncryptionException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
