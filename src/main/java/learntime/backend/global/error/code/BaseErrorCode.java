package learntime.backend.global.error.code;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
