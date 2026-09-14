package learntime.backend.domain.study_member.event;

public record StudyJoinRequestApprovedEvent(
        Long studyJoinRequestId,
        Long studyId,
        String studyTitle,
        Long requesterUserId
) {
}
