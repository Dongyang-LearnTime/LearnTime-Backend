package learntime.backend.global.error;

import learntime.backend.global.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

// 전역 예외 관리
@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("비즈니스 예외 발생: {} - {}", errorCode.getCode(), errorCode.getMessage());

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

    // 설정한 예외 외의 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handle(Exception error) {
        log.error("Unhandled Exception: ", error);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDTO.builder()
                        .errorCode("500")
                        .message("서버 오류입니다. 잠시 후 다시 접속해주세요.")
                        .detail(error.getMessage())
                        .build());
    }

    // ResponseStatusException 처리 (4xx, 5xx)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleStatus(ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(ErrorResponseDTO.builder()
                        .errorCode(String.valueOf(e.getStatusCode().value()))
                        .message(e.getReason() != null ? e.getReason() : "요청 처리 중 오류가 발생했습니다.")
                        .detail("")
                        .build());
    }
}
