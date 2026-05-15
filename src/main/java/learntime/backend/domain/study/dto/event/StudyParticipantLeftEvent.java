package learntime.backend.domain.study.dto.event;

public record StudyParticipantLeftEvent(
        Long studyId,
        String studyTitle,
        Long ownerId,
        Long memberId,
        String memberName
) {
}
