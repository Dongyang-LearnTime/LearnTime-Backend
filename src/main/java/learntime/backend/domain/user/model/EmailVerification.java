package learntime.backend.domain.user.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_verification",
        indexes = {
                @Index(name = "idx_email_verification_email_created", columnList = "email, created_at"),
                @Index(name = "idx_email_verification_token", columnList = "verification_token_hash")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long emailVerificationId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 128)
    private String codeHash;

    @Column(length = 128)
    private String verificationTokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime verifiedAt;

    @Column
    private LocalDateTime consumedAt;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private EmailVerification(String email, String codeHash, LocalDateTime expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public static EmailVerification create(String email, String codeHash, LocalDateTime expiresAt) {
        return new EmailVerification(email, codeHash, expiresAt);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void increaseAttemptCount() {
        this.attemptCount++;
    }

    public void markVerified(String verificationTokenHash) {
        this.verificationTokenHash = verificationTokenHash;
        this.verifiedAt = LocalDateTime.now();
    }

    public void markConsumed() {
        this.consumedAt = LocalDateTime.now();
    }
}
