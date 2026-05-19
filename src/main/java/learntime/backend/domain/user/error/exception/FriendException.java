package learntime.backend.domain.user.error.exception;

import learntime.backend.domain.user.error.code.FriendErrorCode;
import learntime.backend.global.error.exception.BaseException;

public class FriendException extends BaseException {
    public FriendException(FriendErrorCode friendErrorCode) {
        super(friendErrorCode);
    }
}
