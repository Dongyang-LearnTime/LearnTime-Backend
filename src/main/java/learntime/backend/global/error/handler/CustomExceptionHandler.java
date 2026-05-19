package learntime.backend.global.error.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import learntime.backend.global.dto.ErrorResponseDTO;
import learntime.backend.global.error.code.BaseErrorCode;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BaseException;
import learntime.backend.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;

// 전역 예외 관리
@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    // 설정한 예외 외의 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handle(Exception error) {
        log.error("Unhandled Exception: ", error); // 서버 내부 로그 (StackTrace 포함)

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO(
                        "500",
                        "서버 오류입니다. 잠시 후 다시 접속해주세요.",
                        ""
                ));
    }

    // 개발자가 만든 에러는 다 여기에
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponseDTO> handleCustomException(BaseException e) {
        log.error("BaseException: ", e);

        BaseErrorCode errorCode = e.getErrorCode();

        ErrorResponseDTO response = new ErrorResponseDTO(
                errorCode.getCode(),
                e.getMessage(),
                ""
        );

        return ResponseEntity.
                status(errorCode.getStatus()).
                body(response);
    }


    // ResponseStatusException 처리 (4xx, 5xx)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleStatus(ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(new ErrorResponseDTO(
                        String.valueOf(e.getStatusCode().value()),
                        e.getReason() != null
                                ? e.getReason()
                                : "요청 처리 중 오류가 발생했습니다.",
                        e.getMessage()
                ));
    }

    // @Valid + @RequestBody 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {

        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(ErrorCode.INVALID_REQUEST_BODY.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST_BODY.getStatus())
                .body(new ErrorResponseDTO(
                        ErrorCode.INVALID_REQUEST_BODY.getCode(),
                        ErrorCode.INVALID_REQUEST_BODY.getMessage(),
                        errorMessage
                ));
    }

    // RequestParam, PathVariable 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolationException(
            ConstraintViolationException e
    ) {

        String errorMessage = e.getConstraintViolations()
                .stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(ErrorCode.INVALID_REQUEST_PARAMETER.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST_PARAMETER.getStatus())
                .body(new ErrorResponseDTO(
                        ErrorCode.INVALID_REQUEST_PARAMETER.getCode(),
                        ErrorCode.INVALID_REQUEST_PARAMETER.getMessage(),
                        errorMessage
                ));
    }

}
