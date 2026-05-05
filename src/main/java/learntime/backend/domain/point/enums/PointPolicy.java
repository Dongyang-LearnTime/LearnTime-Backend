package learntime.backend.domain.point.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointPolicy {

    STUDY_PLAN_CREATED(10, "공부 일정 생성"),
    STUDY_COMPLETED_SUCCESS(10, "일일 진도 완료 (성공)"),
    STUDY_COMPLETED_FAILURE(2, "일일 진도 완료 (실패)"), // 실패 시 지급할 최소 격려 포인트 추가
    STUDY_QUIZ_COMPLETED(10, "퀴즈 완료");

    private final int amount;
    private final String description;
}