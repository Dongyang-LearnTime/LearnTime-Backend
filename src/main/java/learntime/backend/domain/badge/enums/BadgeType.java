package learntime.backend.domain.badge.enums;

import learntime.backend.domain.badge.model.UserStats;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public enum BadgeType {

    // 1. 일정 관련 배지
    FIRST_STEP("앞으로 한 걸음", "최초로 일정 계획 및 완료",
            stats -> stats.getValue(StatKey.CONSECUTIVE_STUDY_DAYS) >= 1),

    DILIGENT_PLANNER("성실한 계획가", "30일 연속 일정 계획 및 완료",
            stats -> stats.getValue(StatKey.CONSECUTIVE_STUDY_DAYS) >= 30),

    EXCELLENT_STRATEGIST("뛰어난 전략가", "90일 연속 일정 계획 및 완료",
            stats -> stats.getValue(StatKey.CONSECUTIVE_STUDY_DAYS) >= 90),

    TRUE_J_MBTI("당신의 MBTI는 대문자 J", "180일 연속 일정 계획 및 완료",
            stats -> stats.getValue(StatKey.CONSECUTIVE_STUDY_DAYS) >= 180),

    // 2. 퀴즈 관련 배지
    HUMAN_GPT("인간 GPT", "퀴즈 10회 연속 만점",
            stats -> stats.getValue(StatKey.CONSECUTIVE_PERFECT_QUIZ) >= 10),

    // 3. 필기 관련 배지
    TRIPITAKA_COREANA("팔십대장경", "필기 80번 이상 업로드 완료",
            stats -> stats.getValue(StatKey.TOTAL_NOTE_COUNT) >= 80),

    // 4. 미라클 모닝 관련 배지
    EARLY_BIRD("일찍 일어나는 새가 벌레를", "오전 8시 전 공부 또는 운동 중 한가지 최초 1회 완료",
            stats -> stats.getValue(StatKey.CONSECUTIVE_MIRACLE_MORNING) >= 1),

    MIRACLE_MORNING_ADDICT("미라클 모닝 중독", "오전 8시 전 공부 또는 운동 중 한가지 완료 연속 5회 달성",
            stats -> stats.getValue(StatKey.CONSECUTIVE_MIRACLE_MORNING) >= 5);

    private final String displayName;
    private final String description;
    private final Predicate<UserStats> condition;

    public boolean isSatisfiedBy(UserStats stats) {
        return this.condition.test(stats);
    }
}
