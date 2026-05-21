package learntime.backend.domain.study_member.event;

public record StudyInvitationRejectedEvent(
        Long studyInvitationId,
        Long studyId,
        String studyTitle,
        String rejectedUserName,
        Long inviterUserId
) {}
