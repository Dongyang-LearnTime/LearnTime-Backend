package learntime.backend.domain.study_member.dto.response;

import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record StudyMemberResponseDTO(
        Long studyMemberId,
        StudyMemberRole studyMemberRole,
        LocalDateTime joinedAt,
        Long userId,
        String userName,
        String profileImageUrl,
        StudyMemberStatus status,
        Boolean hasBlocked
) {
}
