package learntime.backend.domain.community.dto.response;

import learntime.backend.domain.point.enums.PointMilestone;
import learntime.backend.domain.user.model.User;

public record PointRankingResponseDTO (
    Long userId,
    String name,
    Integer point,
    String tierName,
    int rank
) { }
