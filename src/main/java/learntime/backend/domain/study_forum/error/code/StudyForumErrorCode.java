package learntime.backend.domain.study_forum.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StudyForumErrorCode implements BaseErrorCode {
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "STUDY-FORUM-001", "토론방에 접근할 권한이 없습니다."),
    READ_ONLY(HttpStatus.FORBIDDEN, "STUDY-FORUM-002", "완료된 스터디에는 메시지를 작성할 수 없습니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-FORUM-003", "토론 메시지를 찾을 수 없습니다."),
    NOT_AUTHOR(HttpStatus.FORBIDDEN, "STUDY-FORUM-004", "본인의 메시지만 삭제할 수 있습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "STUDY-FORUM-005", "메시지 또는 조회 범위가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

