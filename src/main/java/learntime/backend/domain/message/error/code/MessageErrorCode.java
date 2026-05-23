package learntime.backend.domain.message.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MessageErrorCode implements BaseErrorCode {

    /** 쪽지가 존재하지 않을 때 사용 */
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSAGE-001", "쪽지를 찾을 수 없습니다."),

    /** 자기 자신에게 쪽지를 보낼 때 사용 */
    CANNOT_SEND_SELF(HttpStatus.BAD_REQUEST, "MESSAGE-002", "자기 자신에게 쪽지를 보낼 수 없습니다."),

    /** 쪽지에 접근할 권한이 없을 때 사용 (본인이 송수신자가 아니거나 이미 양측에서 삭제된 경우 등) */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "MESSAGE-003", "해당 쪽지에 접근할 권한이 없습니다.");

    /** HTTP 응답 상태 */
    private final HttpStatus status;

    /** 클라이언트가 식별할 수 있는 에러 코드 */
    private final String code;

    /** 사용자에게 보여줄 에러 메시지 */
    private final String message;

    MessageErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
