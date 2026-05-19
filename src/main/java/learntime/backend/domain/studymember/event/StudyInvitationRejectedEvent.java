package learntime.backend.domain.studymember.event;

public record StudyInvitationRejectedEvent(
        Long studyInvitationId,
        Long studyId,
        String studyTitle,
        String rejectedUserName,
        Long inviterUserId
) {}
