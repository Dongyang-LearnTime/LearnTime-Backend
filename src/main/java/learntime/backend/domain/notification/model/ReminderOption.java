package learntime.backend.domain.notification.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public enum ReminderOption {
    // 리마인더 기준 시간 커스텀 (상수로 관리)
    NONE("알림 없음", 0),
    ON_TIME("정시", 0),
    TEN_MINUTES_BEFORE("10분 전", 10),
    THIRTY_MINUTES_BEFORE("30분 전", 30),
    ONE_HOUR_BEFORE("1시간 전", 60);

    private final String description;
    private final long minutesBefore;

    // 실제 알람이 울릴 시간 계산
    public LocalDateTime calculateRemindAt(LocalDateTime targetDate) {
        if (this == NONE) return null;
        return targetDate.minusMinutes(this.minutesBefore);
    }
}
