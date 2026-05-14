package learntime.backend.domain.community.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommunityErrorCode implements BaseErrorCode  {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST-001", "게시글을 찾을 수 없습니다."),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "POST-001", "이미지는 최대 3개까지 첨부할 수 있습니다."),

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_001", "댓글을 찾을 수 없습니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

    CommunityErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
