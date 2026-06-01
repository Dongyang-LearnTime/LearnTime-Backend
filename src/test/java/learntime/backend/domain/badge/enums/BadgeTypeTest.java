package learntime.backend.domain.badge.enums;

import learntime.backend.domain.badge.model.UserActivityStat;
import learntime.backend.domain.badge.model.UserStats;
import learntime.backend.domain.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BadgeTypeTest {

    private User mockUser() {
        return User.builder()
                .email("test@test.com")
                .name("테스터")
                .build();
    }

    private UserActivityStat createStat(User user, StatKey key, int value) {
        UserActivityStat stat = UserActivityStat.builder()
                .user(user)
                .statKey(key)
                .build();
        for (int i = 0; i < value; i++) {
            stat.incrementValue();
        }
        return stat;
    }

    @Test
    @DisplayName("일정 관련 배지 획득 조건 검증")
    void checkStudyBadges() {
        User user = mockUser();

        // 1. 앞으로 한 걸음 (1일 연속 완료)
        UserStats stats1 = new UserStats(user, List.of(createStat(user, StatKey.CONSECUTIVE_STUDY_DAYS, 1)));
        assertThat(BadgeType.FIRST_STEP.isSatisfiedBy(stats1)).isTrue();
        assertThat(BadgeType.DILIGENT_PLANNER.isSatisfiedBy(stats1)).isFalse();

        // 2. 성실한 계획가 (30일 연속 완료)
        UserStats stats30 = new UserStats(user, List.of(createStat(user, StatKey.CONSECUTIVE_STUDY_DAYS, 30)));
        assertThat(BadgeType.FIRST_STEP.isSatisfiedBy(stats30)).isTrue();
        assertThat(BadgeType.DILIGENT_PLANNER.isSatisfiedBy(stats30)).isTrue();
        assertThat(BadgeType.EXCELLENT_STRATEGIST.isSatisfiedBy(stats30)).isFalse();

        // 3. 뛰어난 전략가 (90일 연속 완료)
        UserStats stats90 = new UserStats(user, List.of(createStat(user, StatKey.CONSECUTIVE_STUDY_DAYS, 90)));
        assertThat(BadgeType.EXCELLENT_STRATEGIST.isSatisfiedBy(stats90)).isTrue();
        assertThat(BadgeType.TRUE_J_MBTI.isSatisfiedBy(stats90)).isFalse();

        // 4. 당신의 MBTI는 대문자 J (180일 연속 완료)
        UserStats stats180 = new UserStats(user, List.of(createStat(user, StatKey.CONSECUTIVE_STUDY_DAYS, 180)));
        assertThat(BadgeType.TRUE_J_MBTI.isSatisfiedBy(stats180)).isTrue();
    }

    @Test
    @DisplayName("인간 GPT (퀴즈 10회 연속 만점) 배지 획득 조건 검증")
    void checkHumanGptBadge() {
        User user = mockUser();

        // 9회 연속 만점: 조건 미달
        UserStats stats9 = new UserStats(user, List.of(createStat(user, StatKey.CONSECUTIVE_PERFECT_QUIZ, 9)));
        assertThat(BadgeType.HUMAN_GPT.isSatisfiedBy(stats9)).isFalse();

        // 10회 연속 만점: 조건 만족
        UserStats stats10 = new UserStats(user, List.of(createStat(user, StatKey.CONSECUTIVE_PERFECT_QUIZ, 10)));
        assertThat(BadgeType.HUMAN_GPT.isSatisfiedBy(stats10)).isTrue();
    }

    @Test
    @DisplayName("팔십대장경 (필기 80번 이상 업로드) 배지 획득 조건 검증")
    void checkTripitakaCoreanaBadge() {
        User user = mockUser();

        // 79번 업로드: 조건 미달
        UserStats stats79 = new UserStats(user, List.of(createStat(user, StatKey.TOTAL_NOTE_COUNT, 79)));
        assertThat(BadgeType.TRIPITAKA_COREANA.isSatisfiedBy(stats79)).isFalse();

        // 80번 업로드: 조건 만족
        UserStats stats80 = new UserStats(user, List.of(createStat(user, StatKey.TOTAL_NOTE_COUNT, 80)));
        assertThat(BadgeType.TRIPITAKA_COREANA.isSatisfiedBy(stats80)).isTrue();
    }

    @Test
    @DisplayName("미라클 모닝 관련 배지 획득 조건 검증")
    void checkMiracleMorningBadges() {
        User user = mockUser();

        // 1. 일찍 일어나는 새가 벌레를 (1회 완료)
        UserStats stats1 = new UserStats(user, List.of(createStat(user, StatKey.CONSECUTIVE_MIRACLE_MORNING, 1)));
        assertThat(BadgeType.EARLY_BIRD.isSatisfiedBy(stats1)).isTrue();
        assertThat(BadgeType.MIRACLE_MORNING_ADDICT.isSatisfiedBy(stats1)).isFalse();

        // 2. 미라클 모닝 중독 (5회 연속 완료)
        UserStats stats5 = new UserStats(user, List.of(createStat(user, StatKey.CONSECUTIVE_MIRACLE_MORNING, 5)));
        assertThat(BadgeType.EARLY_BIRD.isSatisfiedBy(stats5)).isTrue();
        assertThat(BadgeType.MIRACLE_MORNING_ADDICT.isSatisfiedBy(stats5)).isTrue();
    }
}
