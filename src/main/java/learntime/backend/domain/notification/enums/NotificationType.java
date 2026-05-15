package learntime.backend.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    /** 친구 요청을 받았을 때 발생하는 알림 */
    FRIEND_REQUEST_RECEIVED("friend-request-received"),

    /** 내가 보낸 친구 요청이 수락되었을 때 발생하는 알림 */
    FRIEND_REQUEST_ACCEPTED("friend-request-accepted"),

    /** 내가 보낸 친구 요청이 거절되었을 때 발생하는 알림 */
    FRIEND_REQUEST_REJECTED("friend-request-rejected"),

    /** 캘린더 일정 리마인더 알림 */
    CALENDAR_REMINDER("calendar-reminder"),

    /** 친구가 공부 일정을 공유했을 때 발생하는 알림 */
    STUDY_SHARED("study-shared"),

    /** 공유 공부 일정에서 참가자가 나갔을 때 발생하는 알림 */
    STUDY_PARTICIPANT_LEFT("study-participant-left");

    /** SSE 클라이언트가 구독하는 이벤트 이름 */
    private final String eventName;
}
