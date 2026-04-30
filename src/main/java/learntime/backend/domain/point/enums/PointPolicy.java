package learntime.backend.domain.point.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointPolicy {

    // 증가만
    STUDY_PLAN_CREATED(10, "공부 일정 생성"),
    STUDY_COMPLETED(10, "일일 진도 완료");

    private final int amount;
    private final String description;
}