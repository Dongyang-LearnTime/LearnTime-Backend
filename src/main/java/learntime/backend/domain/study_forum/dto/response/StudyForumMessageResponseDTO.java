package learntime.backend.domain.study_forum.dto.response;

import java.time.LocalDateTime;

public record StudyForumMessageResponseDTO(
        Long messageId, Long authorStudyMemberId, Long authorUserId, String authorName, String content, LocalDateTime createdAt
) {}

