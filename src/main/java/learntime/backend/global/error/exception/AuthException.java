package learntime.backend.global.error.exception;

import learntime.backend.global.error.code.AuthErrorCode;
import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    private final AuthErrorCode authErrorCode;

    public AuthException(AuthErrorCode authErrorCode) {
        super(authErrorCode.getMessage());
        this.authErrorCode = authErrorCode;
    }
}