package learntime.backend.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    // 친구 요청을 받았을 때 발생하는 알림
    FRIEND_REQUEST_RECEIVED("friend-request-received"),

    // 내가 보낸 친구 요청이 수락되었을 때 발생하는 알림
    FRIEND_REQUEST_ACCEPTED("friend-request-accepted"),

    //내가 보낸 친구 요청이 거절되었을 때 발생하는 알림
    FRIEND_REQUEST_REJECTED("friend-request-rejected"),

    // 캘린더 일정 리마인더 알림
    CALENDAR_REMINDER("calendar-reminder"),

    // 스터디 초대를 받았을 때 발생하는 알림
    STUDY_INVITATION_RECEIVED("study-invitation-received"),

    // 내가 보낸 스터디 초대가 수락되었을 때 발생하는 알림
    STUDY_INVITATION_ACCEPTED("study-invitation-accepted"),

    // 내가 보낸 스터디 초대가 거절되었을 때 발생하는 알림
    STUDY_INVITATION_REJECTED("study-invitation-rejected"),

    // 스터디 가입 요청을 받았을 때 발생하는 알림 (방장)
    STUDY_JOIN_REQUEST_RECEIVED("study-join-request-received"),

    // 내가 보낸 스터디 가입 요청이 승인되었을 때 발생하는 알림 (요청자)
    STUDY_JOIN_REQUEST_APPROVED("study-join-request-approved"),

    // 내가 보낸 스터디 가입 요청이 거절되었을 때 발생하는 알림 (요청자)
    STUDY_JOIN_REQUEST_REJECTED("study-join-request-rejected"),

    // 쪽지를 받았을 때 발생하는 알림
    MESSAGE_RECEIVED("message-received");



    // SSE 클라이언트가 구독하는 이벤트 이름
    private final String eventName;
}
