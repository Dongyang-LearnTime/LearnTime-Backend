package learntime.backend.domain.point.dto;

import learntime.backend.domain.point.enums.PointMilestone;
import learntime.backend.domain.user.model.User;

public record PointRankingResponseDTO (
    Long userId,
    String name,
    Integer point,
    String tierName,
    int rank
) {
    public static PointRankingResponseDTO from(User user, int rank) {
        return new PointRankingResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getPoint(),
                PointMilestone.getTier(user.getPoint()).getTierName(),
                rank
        );
    }
}
