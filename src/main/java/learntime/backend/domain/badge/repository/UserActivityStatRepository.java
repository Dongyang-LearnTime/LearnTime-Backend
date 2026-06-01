package learntime.backend.domain.badge.repository;

import learntime.backend.domain.badge.model.UserActivityStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityStatRepository extends JpaRepository<UserActivityStat, Long> {
    List<UserActivityStat> findAllByUser_UserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserActivityStat s WHERE s.user.userId = :userId")
    List<UserActivityStat> findAllByUser_UserIdForUpdate(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserActivityStat uas WHERE uas.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
