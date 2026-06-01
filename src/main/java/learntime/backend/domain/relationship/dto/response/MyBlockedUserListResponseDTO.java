package learntime.backend.domain.relationship.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MyBlockedUserListResponseDTO(
        Long userBlockId,           // 차단 ID
        Long blockedUserId,         // 차단한 사용자의 User ID
        String blockedUserName,     // 차단한 사용자의 이름
        LocalDateTime blockedAt     // 차단일
) { }
