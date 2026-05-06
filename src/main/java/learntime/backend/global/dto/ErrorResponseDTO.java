package learntime.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record ErrorResponseDTO(

        String errorCode, // 에러 코드
        String message,   // 사용자 메시지
        String detail,    // 로그용 메시지

        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss",
                timezone = "Asia/Seoul"
        )
        LocalDateTime timestamp // 생성 시각
) {

    public ErrorResponseDTO(String errorCode, String message, String detail) {
        this(errorCode, message, detail, LocalDateTime.now());
    }
}
