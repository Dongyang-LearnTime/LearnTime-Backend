package learntime.backend.domain.community.dto.response;

import lombok.Builder;

@Builder
public record PointRankingResponseDTO (
    Long userId,
    String name,
    Integer point,
    String tierName,
    int rank,
    Boolean hasBlocked
) { }
