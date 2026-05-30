package learntime.backend.domain.point.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointPolicy {

    STUDY_PLAN_CREATED(10, "공부 일정 생성"),
    STUDY_PLAN_JOINED(10, "공부 일정 참여"),
    STUDY_COMPLETED_SUCCESS(10, "일일 진도 완료 (성공)"),
    STUDY_COMPLETED_FAILURE(2, "일일 진도 완료 (실패)"), // 실패 시 지급할 최소 격려 포인트 추가
    STUDY_QUIZ_COMPLETED(10, "퀴즈 완료"),
    EXERCISE_DAILY_COMPLETED(10, "일일 운동 기록 완료"),
    EXERCISE_CONSECUTIVE_3_DAYS(50, "운동 3일 연속 달성 보너스"),
    EXERCISE_CONSECUTIVE_7_DAYS(100, "운동 7일 연속 달성 보너스"),
    EXERCISE_CONSECUTIVE_30_DAYS(500, "운동 30일 연속 달성 보너스");

    private final int amount;
    private final String description;
}
