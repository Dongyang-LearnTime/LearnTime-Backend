package learntime.backend.domain.studymember.dto.response;

import learntime.backend.domain.studymember.enums.StudyMemberRole;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record StudyMemberResponseDTO(
        Long studyMemberId,
        StudyMemberRole studyMemberRole,
        LocalDateTime joinedAt,
        Long userId,
        String userName
) {
}
