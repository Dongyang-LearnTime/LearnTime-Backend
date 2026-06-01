package learntime.backend.domain.badge.repository;

import learntime.backend.domain.badge.enums.BadgeType;
import learntime.backend.domain.badge.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    @Query("SELECT ub.badgeType FROM UserBadge ub WHERE ub.user.userId = :userId")
    List<BadgeType> findBadgeTypesByUserId(@Param("userId") Long userId);

    @Query("SELECT ub FROM UserBadge ub WHERE ub.user.userId = :userId ORDER BY ub.acquiredAt DESC")
    List<UserBadge> findAllByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserBadge ub WHERE ub.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}

