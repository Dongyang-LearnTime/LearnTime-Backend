package learntime.backend.domain.friend.repository;

import learntime.backend.domain.friend.model.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// 친구 관계의 존재 여부, 목록 조회, 단건 조회를 담당하는 Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    /** 이미 친구 사이인지 확인하기 (Spring Data JPA 기본 제공 기능 활용) */
    boolean existsByUser_UserIdAndFriendUser_UserId(Long userId, Long friendUserId);

    default boolean existsFriendship(Long userId, Long friendUserId) {
        return existsByUser_UserIdAndFriendUser_UserId(userId, friendUserId) ||
               existsByUser_UserIdAndFriendUser_UserId(friendUserId, userId);
    }

    /** 내 모든 친구 목록 가져오기 */
    @Query("""
            SELECT f
            FROM Friend f
            WHERE f.user.userId = :userId OR f.friendUser.userId = :userId
            ORDER BY f.createdAt DESC
            """)
    List<Friend> findAllByUserId(@Param("userId") Long userId);

    /** 내 친구 수 가져오기 */
    @Query("""
            SELECT COUNT(f)
            FROM Friend f
            WHERE f.user.userId = :userId OR f.friendUser.userId = :userId
            """)
    long countFriendsByUserId(@Param("userId") Long userId);

    /** 특정 사람과의 친구 관계 데이터 찾기 */
    @Query("""
            SELECT f
            FROM Friend f
            WHERE (f.user.userId = :userId AND f.friendUser.userId = :friendUserId)
               OR (f.user.userId = :friendUserId AND f.friendUser.userId = :userId)
            """)
    Optional<Friend> findFriendship(@Param("userId") Long userId, @Param("friendUserId") Long friendUserId);


    /** 양방향 친구 여부 확인 */
    @Query("""
    SELECT COUNT(f) > 0
    FROM Friend f
    WHERE
        (f.user.userId = :userId1 AND f.friendUser.userId = :userId2)
        OR
        (f.user.userId = :userId2 AND f.friendUser.userId = :userId1)
    """)
    boolean existsFriendRelation(
            Long userId1,
            Long userId2
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM Friend f
            WHERE f.user.userId = :userId
               OR f.friendUser.userId = :userId
            """)
    void deleteAllByUserId(@Param("userId") Long userId);

}
