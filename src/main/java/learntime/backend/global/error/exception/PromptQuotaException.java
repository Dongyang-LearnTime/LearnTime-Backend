package learntime.backend.global.error.exception;

import learntime.backend.global.error.code.ErrorCode;

public class PromptQuotaException extends BusinessException {
    public PromptQuotaException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
