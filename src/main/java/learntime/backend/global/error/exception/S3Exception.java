package learntime.backend.global.error.exception;

import learntime.backend.global.error.code.BaseErrorCode;

public class S3Exception extends BaseException {
    public S3Exception(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
