package learntime.backend.domain.study_member.event;

public record StudyJoinRequestCreatedEvent(
        Long studyJoinRequestId,
        Long studyId,
        String studyTitle,
        String requesterName,
        Long ownerUserId
) {
}
