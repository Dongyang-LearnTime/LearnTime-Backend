package learntime.backend.domain.point.dto;

import learntime.backend.domain.point.enums.PointType;

public record PointEventDTO(
        Long userId,
        int amount,
        PointType pointType,
        String description
) {}
