package learntime.backend.global.error.exception;

import learntime.backend.global.error.code.AuthErrorCode;

public class AuthException extends BaseException {
    public AuthException(AuthErrorCode authErrorCode) {
        super(authErrorCode);
    }
}
