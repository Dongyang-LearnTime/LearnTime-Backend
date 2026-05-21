package learntime.backend.domain.study_member.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record StudyInvitationResponseDTO(
        Long studyInvitationId,
        Long studyId,
        String studyTitle,
        Long userId,
        String userName,
        LocalDateTime requestedAt
) {
}
