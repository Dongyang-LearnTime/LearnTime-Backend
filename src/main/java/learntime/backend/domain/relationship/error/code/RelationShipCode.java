package learntime.backend.domain.relationship.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum RelationShipCode implements BaseErrorCode {
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND-001", "친구 요청을 찾을 수 없습니다."),
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND-002", "친구 관계를 찾을 수 없습니다."),
    CANNOT_REQUEST_SELF(HttpStatus.BAD_REQUEST, "FRIEND-003", "자기 자신에게 친구 요청을 보낼 수 없습니다."),
    FRIEND_ALREADY_EXISTS(HttpStatus.CONFLICT, "FRIEND-004", "이미 친구 관계입니다."),
    FRIEND_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "FRIEND-005", "이미 대기 중인 친구 요청이 있습니다."),
    FRIEND_REQUEST_CANCEL_FORBIDDEN(HttpStatus.FORBIDDEN, "FRIEND-006", "본인이 보낸 친구 요청만 취소할 수 있습니다."),

    USER_TO_BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "BLOCK-001", "차단할 사용자를 찾을 수 없습니다."),
    USER_TO_UNBLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "BLOCK-002", "차단 해제할 사용자를 찾을 수 없습니다."),
    FRIEND_CANNOT_BE_BLOCKED(HttpStatus.BAD_REQUEST, "BLOCK-003", "친구인 사용자는 차단할 수 없습니다."),
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "BLOCK-004", "자기 자신은 차단할 수 없습니다."),
    USER_ALREADY_BLOCKED(HttpStatus.CONFLICT, "BLOCK-005", "이미 차단한 사용자입니다."),
    BLOCKED_BY_USER(HttpStatus.FORBIDDEN, "BLOCK-006", "해당 사용자에게 차단되어 요청을 수행할 수 없습니다.")
    ;




    private final HttpStatus status;
    private final String code;
    private final String message;

    RelationShipCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
