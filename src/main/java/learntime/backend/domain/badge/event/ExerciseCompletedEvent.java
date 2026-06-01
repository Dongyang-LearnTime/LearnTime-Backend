package learntime.backend.domain.badge.event;

import java.time.LocalDateTime;

public record ExerciseCompletedEvent(
        Long userId,
        LocalDateTime completedAt
) {}
