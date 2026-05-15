package learntime.backend.domain.study.dto.event;

public record StudySharedEvent(
        Long studyId,
        String studyTitle,
        Long ownerId,
        String ownerName,
        Long memberId
) {
}
