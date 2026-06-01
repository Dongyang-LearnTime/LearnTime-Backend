package learntime.backend.domain.relationship.repository;

import learntime.backend.domain.relationship.model.UserBlock;
import learntime.backend.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    // 내가 차단한 사용자의 id
    @Query("""
        SELECT ub.blocked.userId
        FROM UserBlock ub
        WHERE ub.blocker.userId = :myUserId
    """)
    Set<Long> findBlockedUserIds(
            @Param("myUserId") Long myUserId
    );

    void deleteByBlocker_UserIdAndBlocked_UserId(
            Long blockerId,
            Long blockedId
    );

    boolean existsByBlockerAndBlocked(
            User blocker,
            User blocked
    );

    // 대상 사용자가 로그인 사용자를 차단했는지 확인
    boolean existsByBlocker_UserIdAndBlocked_UserId(
            Long targetUserId,
            Long loginUserId
    );

    @Query("""
        SELECT ub
        FROM UserBlock ub
        JOIN FETCH ub.blocked b
        WHERE ub.blocker.userId = :userId
        ORDER BY ub.createdAt DESC
    """)
    Page<UserBlock> findBlockedUsers(
            @Param("userId") Long userId,
            Pageable pageable
    );

    // 특정 사용자가 차단한 모든 관계 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM UserBlock ub
            WHERE ub.blocker.userId = :blockerId
            """)
    int deleteAllByBlockerId(Long blockerId);

    // 특정 사용자가 차단당한 모든 관계 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM UserBlock ub
            WHERE ub.blocked.userId = :blockedId
            """)
    int deleteAllByBlockedId(Long blockedId);

}
