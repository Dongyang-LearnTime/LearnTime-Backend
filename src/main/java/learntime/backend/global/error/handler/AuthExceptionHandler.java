package learntime.backend.global.error.handler;

import learntime.backend.global.dto.ErrorResponseDTO;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusiness(AuthException e) {
        AuthErrorCode errorCode = e.getAuthErrorCode();

        ErrorResponseDTO body = new ErrorResponseDTO(
                errorCode.getCode(),
                errorCode.getMessage(),
                "",
                LocalDateTime.now()
        );

        return ResponseEntity.
                status(errorCode.getStatus()).
                body(body);
    }


    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponseDTO> handleLockedException(LockedException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseDTO.builder()
                        .errorCode("403")
                        .message("비밀번호 5회 오류로 계정이 잠겼습니다.")
                        .detail(e.getMessage())
                        .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponseDTO.builder()
                        .errorCode("401")
                        .message("비밀번호가 일치하지 않습니다.")
                        .detail(e.getMessage())
                        .build());
    }

}
