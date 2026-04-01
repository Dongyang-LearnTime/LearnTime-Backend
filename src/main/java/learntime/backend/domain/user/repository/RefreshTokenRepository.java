package learntime.backend.domain.user.repository;

import learntime.backend.domain.user.model.RefreshToken;
import learntime.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByUser(User user);

    // 동시성 문제를 회피하기 위해 DB 엔진 차원의 Upsert 기능 사용
    // 토큰이 있으면 업데이트, 없으면 생성
    @Modifying
    @Query(value = "INSERT INTO refresh_token (user_id, token, expiry_date) VALUES (:userId, :token, :expiryDate) " +
            "ON DUPLICATE KEY UPDATE token = :token, expiry_date = :expiryDate", nativeQuery = true)
    void upsertToken(@Param("userId") Long userId, @Param("token") String token, @Param("expiryDate") LocalDateTime expiryDate);
}
