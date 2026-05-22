package learntime.backend.domain.user.repository;

import learntime.backend.domain.user.enums.FriendRequestStatus;
import learntime.backend.domain.user.model.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// 친구 요청의 중복 확인과 상태별 요청 조회를 담당하는 Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    /** 특정 요청자와 수신자 간의 특정 상태인 친구 요청 존재 여부 확인 */
    boolean existsByRequester_UserIdAndReceiver_UserIdAndStatus(
            Long requesterId,
            Long receiverId,
            FriendRequestStatus status
    );

    /** 특정 수신자의 친구 요청 단건 조회 (특정 상태 조건) */
    Optional<FriendRequest> findByFriendRequestIdAndReceiver_UserIdAndStatus(
            Long friendRequestId,
            Long receiverId,
            FriendRequestStatus status
    );

    /** 특정 요청자가 보낸 친구 요청 단건 조회 (특정 상태 조건) */
    Optional<FriendRequest> findByFriendRequestIdAndRequester_UserIdAndStatus(
            Long friendRequestId,
            Long requesterId,
            FriendRequestStatus status
    );

    /** 특정 수신자가 받은 특정 상태의 친구 요청 목록을 최신순으로 조회 */
    List<FriendRequest> findAllByReceiver_UserIdAndStatusOrderByCreatedAtDesc(
            Long receiverId,
            FriendRequestStatus status
    );

    /** 특정 요청자가 보낸 특정 상태의 친구 요청 목록을 최신순으로 조회 */
    List<FriendRequest> findAllByRequester_UserIdAndStatusOrderByCreatedAtDesc(
            Long requesterId,
            FriendRequestStatus status
    );

    @Modifying
    @Query("DELETE FROM FriendRequest f WHERE f.status = :status AND f.updatedAt <= :cutoffDate")
    void deleteOldRequestsByStatus(
            @Param("status") FriendRequestStatus status,
            @Param("cutoffDate") java.time.LocalDateTime cutoffDate
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM FriendRequest f
            WHERE f.requester.userId = :userId
               OR f.receiver.userId = :userId
            """)
    void deleteAllByUserId(@Param("userId") Long userId);

}
