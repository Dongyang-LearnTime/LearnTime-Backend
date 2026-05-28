package learntime.backend.domain.user.repository;

import learntime.backend.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByName(String name);
    boolean existsByEmail(String email);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.point = u.point + :amount WHERE u.userId = :userId")
    void updatePoint(@Param("userId") Long userId, @Param("amount") int amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.email = :email,
                u.name = :name,
                u.password = null,
                u.socialId = null,
                u.deletedAt = :deletedAt
            WHERE u.userId = :userId
            """)
    void anonymizeAndSoftDelete(
            @Param("userId") Long userId,
            @Param("email") String email,
            @Param("name") String name,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    @Query("SELECT u.userId FROM User u WHERE u.name LIKE %:keyword% ORDER BY u.userId DESC")
    List<Long> findUserIdsByNameContaining(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u.userId FROM User u WHERE u.name LIKE %:keyword% AND u.userId < :lastUserId ORDER BY u.userId DESC")
    List<Long> findUserIdsByNameContainingWithCursor(@Param("keyword") String keyword, @Param("lastUserId") Long lastUserId, Pageable pageable);
}
