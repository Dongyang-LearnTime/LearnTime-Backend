package learntime.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorResponseDTO {
    private final String errorCode; // 에러 코드
    private final String message; // 사용자에게 보여줄 에러 메시지
    private final String detail; // 로그에 찍을 에러 메시지
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private final LocalDateTime timestamp;

    @Builder
    public ErrorResponseDTO(String errorCode, String message, String detail) {
        this.errorCode = errorCode;
        this.message = message;
        this.detail = detail;
        this.timestamp = LocalDateTime.now(); // 생성 시점에 서버 시간 기록
    }

}
