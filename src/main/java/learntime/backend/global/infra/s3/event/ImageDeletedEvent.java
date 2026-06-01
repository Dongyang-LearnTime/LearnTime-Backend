package learntime.backend.global.infra.s3.event;

public record ImageDeletedEvent(
        String imageUrl
) {
}
