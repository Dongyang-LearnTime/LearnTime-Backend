package learntime.backend.domain.point.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PointMilestone {
    BICYCLE("자전거", 0),
    CAR("자동차", 1000),
    HELICOPTER("헬리콥터", 5000),
    AIRPLANE("비행기", 10000),
    SPACESHIP("우주선", 50000);

    private final String tierName;
    private final int minPoint;

    public static PointMilestone getTier(int point) {
        return Arrays.stream(PointMilestone.values())
                .filter(tier -> point >= tier.minPoint)
                .reduce((first, second) -> second)
                .orElse(BICYCLE);
    }
}
