package learntime.backend.domain.badge.event;

import java.time.LocalDateTime;

public record QuizCompletedEvent(
        Long userId,
        boolean isPerfect,
        LocalDateTime completedAt
) {}
