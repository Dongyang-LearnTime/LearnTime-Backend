package learntime.backend.domain.study_member.event;

public record StudyInvitationAcceptedEvent(
        Long studyInvitationId,
        Long studyId,
        String studyTitle,
        String acceptedUserName,
        Long inviterUserId
) {}
