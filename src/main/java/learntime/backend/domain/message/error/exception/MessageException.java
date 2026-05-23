package learntime.backend.domain.message.error.exception;

import learntime.backend.domain.message.error.code.MessageErrorCode;
import learntime.backend.global.error.exception.BaseException;

public class MessageException extends BaseException {
    public MessageException(MessageErrorCode messageErrorCode) {
        super(messageErrorCode);
    }
}
