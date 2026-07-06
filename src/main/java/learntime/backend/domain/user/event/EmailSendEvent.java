package learntime.backend.domain.user.event;

public record EmailSendEvent(String email, String authCode) {
}
