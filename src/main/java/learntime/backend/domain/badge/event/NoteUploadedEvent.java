package learntime.backend.domain.badge.event;

import java.time.LocalDateTime;

public record NoteUploadedEvent(
        Long userId,
        LocalDateTime uploadedAt
) {}
