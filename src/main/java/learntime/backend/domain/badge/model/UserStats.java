package learntime.backend.domain.badge.model;

import learntime.backend.domain.badge.enums.StatKey;
import learntime.backend.domain.user.model.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserStats {

    private final User user;
    private final Map<StatKey, UserActivityStat> statsMap;

    public UserStats(User user, List<UserActivityStat> statList) {
        this.user = user;
        this.statsMap = statList.stream()
                .collect(Collectors.toMap(UserActivityStat::getStatKey, stat -> stat));
    }

    public UserActivityStat getStat(StatKey key) {
        return statsMap.computeIfAbsent(key, k -> UserActivityStat.builder()
                .user(user)
                .statKey(k)
                .build());
    }

    public long getValue(StatKey key) {
        return statsMap.containsKey(key) ? statsMap.get(key).getStatValue() : 0L;
    }

    public List<UserActivityStat> getAllStats() {
        return List.copyOf(statsMap.values());
    }
}
