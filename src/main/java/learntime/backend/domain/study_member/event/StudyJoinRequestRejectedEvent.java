package learntime.backend.domain.study_member.event;

public record StudyJoinRequestRejectedEvent(
        Long studyJoinRequestId,
        Long studyId,
        String studyTitle,
        Long requesterUserId
) {
}
