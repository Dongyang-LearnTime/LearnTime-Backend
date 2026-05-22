package learntime.backend.domain.user.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FriendErrorCode implements BaseErrorCode {
    /** 친구 요청이 존재하지 않을 때 사용 */
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND-001", "친구 요청을 찾을 수 없습니다."),

    /** 친구 관계가 존재하지 않을 때 사용 */
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND-002", "친구 관계를 찾을 수 없습니다."),

    /** 자기 자신에게 친구 요청을 보낼 때 사용 */
    CANNOT_REQUEST_SELF(HttpStatus.BAD_REQUEST, "FRIEND-003", "자기 자신에게 친구 요청을 보낼 수 없습니다."),

    /** 이미 친구 관계가 존재할 때 사용 */
    FRIEND_ALREADY_EXISTS(HttpStatus.CONFLICT, "FRIEND-004", "이미 친구 관계입니다."),

    /** 양방향 중 하나라도 대기 중인 친구 요청이 있을 때 사용 */
    FRIEND_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "FRIEND-005", "이미 대기 중인 친구 요청이 있습니다."),

    /** 친구 요청을 보낸 본인이 아닌 경우 취소 시도 시 사용 */
    FRIEND_REQUEST_CANCEL_FORBIDDEN(HttpStatus.FORBIDDEN, "FRIEND-006", "본인이 보낸 친구 요청만 취소할 수 있습니다.");

    /** HTTP 응답 상태 */
    private final HttpStatus status;

    /** 클라이언트가 식별할 수 있는 에러 코드 */
    private final String code;

    /** 사용자에게 보여줄 에러 메시지 */
    private final String message;

    FriendErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
