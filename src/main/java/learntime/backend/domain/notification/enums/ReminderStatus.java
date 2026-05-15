package learntime.backend.domain.notification.enums;

public enum ReminderStatus {
    WAITING, // 아직 발송 안됨
    SENT, // 정상 발송
    CANCEL // 발송 취소
}