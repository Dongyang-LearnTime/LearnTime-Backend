package learntime.backend.domain.user.repository;

import learntime.backend.domain.user.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findTopByEmailOrderByEmailVerificationIdDesc(String email);

    Optional<EmailVerification> findTopByEmailAndVerificationTokenHashAndConsumedAtIsNullOrderByEmailVerificationIdDesc(
            String email,
            String verificationTokenHash
    );

    int countByEmailAndCreatedAtAfter(String email, LocalDateTime createdAt);

    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
