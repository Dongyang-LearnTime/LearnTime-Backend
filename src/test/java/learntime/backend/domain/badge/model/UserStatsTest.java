package learntime.backend.domain.badge.model;

import learntime.backend.domain.badge.enums.StatKey;
import learntime.backend.domain.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserStatsTest {

    @Test
    @DisplayName("UserStats 생성 및 기본값 조회 동작 확인")
    void testUserStatsBasic() {
        // given
        User user = User.builder().email("test@test.com").build();
        try {
            Field field = User.class.getDeclaredField("userId");
            field.setAccessible(true);
            field.set(user, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<UserActivityStat> emptyStats = new ArrayList<>();
        UserStats userStats = new UserStats(user, emptyStats);

        // when & then
        // 1. 존재하지 않는 통계 키 조회 시 0 반환
        assertThat(userStats.getValue(StatKey.CONSECUTIVE_STUDY_DAYS)).isEqualTo(0L);

        // 2. 존재하지 않는 통계 키 getStat 시 새로 초기화 및 생성됨
        UserActivityStat createdStat = userStats.getStat(StatKey.CONSECUTIVE_STUDY_DAYS);
        assertThat(createdStat).isNotNull();
        assertThat(createdStat.getStatKey()).isEqualTo(StatKey.CONSECUTIVE_STUDY_DAYS);
        assertThat(createdStat.getStatValue()).isEqualTo(0L);
        assertThat(createdStat.getUser()).isEqualTo(user);

        // 3. getAllStats contains the newly computed stat
        assertThat(userStats.getAllStats()).contains(createdStat);
    }
}
