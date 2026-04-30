package learntime.backend.global.error.handler;

import learntime.backend.global.dto.ErrorResponseDTO;
import learntime.backend.global.error.code.BaseErrorCode;
import learntime.backend.global.error.exception.BaseException;
import learntime.backend.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
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

    // @Valid 범위값 오류
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {

        // 첫 번째 에러 메시지를 추출
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("유효하지 않은 입력값입니다.");

        // 2. HTTP 400에 맞는 규격화된 응답 반환
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(
                        "400",
                        "잘못된 요청입니다. 입력값을 확인해주세요.",
                        errorMessage
                ));
    }

}
