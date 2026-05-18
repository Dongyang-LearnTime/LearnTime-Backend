package learntime.backend.domain.studymember.event;

public record StudyInvitationAcceptedEvent(
        Long studyInvitationId,
        Long studyId,
        String studyTitle,
        String acceptedUserName,
        Long inviterUserId
) {}
