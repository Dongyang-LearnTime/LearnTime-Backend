package learntime.backend.domain.study.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FileErrorCode implements BaseErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "FILE-001", "이미지 파일을 첨부해주세요."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "FILE-002", "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE-003", "파일 크기가 제한을 초과했습니다."),
    FILE_CONTENT_MISMATCH(HttpStatus.BAD_REQUEST, "FILE-004", "변조된 파일입니다. 실제 파일 형식이 이미지가 아닙니다."),
    FILE_NAME_INVALID(HttpStatus.BAD_REQUEST, "FILE-005", "잘못된 파일명 형식입니다."),
    FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-006", "파일 읽기 중 서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    FileErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
