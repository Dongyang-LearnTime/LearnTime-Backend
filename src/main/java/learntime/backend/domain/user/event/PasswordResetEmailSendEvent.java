package learntime.backend.domain.user.event;

public record PasswordResetEmailSendEvent(String email, String authCode) {
}
