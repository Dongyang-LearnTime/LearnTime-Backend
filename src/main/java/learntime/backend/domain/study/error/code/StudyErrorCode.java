package learntime.backend.domain.study.error.code;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum StudyErrorCode implements BaseErrorCode {
    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-001", "공부 진도를 찾을 수 없습니다."),
    STUDY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-002", "공부 맴버를 찾을 수 없습니다."),
    STUDY_DAILY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-003", "공부 일일 진도를 찾을 수 없습니다."),

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "STUDY-004", "종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_STUDY_PERIOD(HttpStatus.BAD_REQUEST, "STUDY-005", "학습 기간은 14일 이상 90일 이하만 가능합니다."),
    STUDY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-006", "학습 계획을 저장하는 도중 데이터베이스 오류가 발생했습니다."),
    STUDY_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-007", "공부 필기를 찾을 수 없습니다."),
    STUDY_UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "STUDY-008", "해당 공부 진도/필기에 대한 접근 권한이 없습니다."),
    QUIZ_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-009", "퀴즈 문제를 찾을 수 없습니다."),
    QUIZ_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-010", "퀴즈 풀이 이력을 찾을 수 없습니다."),
    STUDY_DAILY_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "STUDY-011", "이미 완료된 공부 일일 일정에는 사용자 내용을 추가할 수 없습니다."),
    STUDY_DAILY_NOT_YET_STARTED(HttpStatus.BAD_REQUEST, "STUDY-012", "아직 시작되지 않은 공부 계획은 완료 처리할 수 없습니다."),
    
    FEEDBACK_NOT_ENOUGH_DATA(HttpStatus.BAD_REQUEST, "STUDY-013", "피드백을 생성하기 위한 학습 데이터가 부족합니다. (최소 1건 필요)"),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-014", "피드백 정보를 찾을 수 없습니다."),
    SELF_INVITATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "STUDY-015", "자기 자신은 스터디에 초대할 수 없습니다."),
    STUDY_MEMBER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "STUDY-016", "스터디 최대 인원은 4명입니다."),
    ALREADY_STUDY_MEMBER(HttpStatus.BAD_REQUEST, "STUDY-017", "이미 해당 스터디의 멤버입니다."),
    STUDY_INVITATION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "STUDY-018", "이미 대기 중인 초대가 존재합니다."),
    NOT_FRIEND_RELATION(HttpStatus.BAD_REQUEST, "STUDY-019", "친구인 사용자만 스터디에 초대할 수 있습니다."),
    STUDY_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-020", "스터디 초대를 찾을 수 없습니다."),
    NOT_INVITED_USER(HttpStatus.FORBIDDEN, "STUDY-021", "초대받은 사용자만 처리할 수 있습니다."),
    INACTIVE_STUDY_MEMBER(HttpStatus.FORBIDDEN, "STUDY-022", "활동하지 않는 스터디 맴버입니다."),
    STUDY_INVITATION_NOT_PENDING(HttpStatus.BAD_REQUEST, "STUDY-023", "대기 중인 초대가 아닙니다."),
    NOT_INVITER_USER(HttpStatus.FORBIDDEN, "STUDY-024", "초대한 사용자만 처리할 수 있습니다."),
    INVALID_OWNER_TRANSFER(HttpStatus.BAD_REQUEST, "STUDY-025", "자기 자신에게 방장 권한을 위임할 수 없습니다."),
    PROMPT_INIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY-026", "AI 프롬프트 템플릿 초기화에 실패했습니다."),
    STUDY_DAILY_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "STUDY-027", "이미 시작되었거나 종료된 계획입니다."),
    STUDY_USER_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-028", "일일 공부 내용을 찾을 수 없습니다."),
    HOLIDAY_REGISTRATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "STUDY-029", "휴무일에는 공부 내용을 등록하거나 수정할 수 없습니다."),
    STUDY_REST_UPDATE_NOT_READY(HttpStatus.BAD_REQUEST, "STUDY-030", "공부 진도 생성이 완료된 후 휴무 일정을 변경할 수 있습니다."),
    TODAY_REST_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "STUDY-031", "오늘을 새 휴무일로 변경할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    StudyErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
