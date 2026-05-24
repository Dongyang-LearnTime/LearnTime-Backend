package learntime.backend.domain.badge.event;

import java.time.LocalDateTime;

public record StudyCompletedEvent(
        Long userId,
        LocalDateTime completedAt
) {}
