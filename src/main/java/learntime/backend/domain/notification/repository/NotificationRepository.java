package learntime.backend.domain.notification.repository;

import learntime.backend.domain.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 알림 목록 첫 조회
    List<Notification> findTop20ByReceiver_UserIdOrderByNotificationIdDesc(Long userId);

    // 알림 목록 다음 페이지 조회 (커서 기반)
    List<Notification> findTop20ByReceiver_UserIdAndNotificationIdLessThanOrderByNotificationIdDesc(
            Long userId,
            Long cursorId
    );


    // 읽지 않은 내 알림 개수 조회
    long countByReceiver_UserIdAndIsReadFalse(Long receiverId);

    // 내 알림 단건을 조회해 다른 사용자의 알림 읽음 처리를 방지
    Optional<Notification> findByNotificationIdAndReceiver_UserId(Long notificationId, Long receiverId);

    // 내 모든 미확인 알림을 한 번의 쿼리로 읽음 처리
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.userId = :receiverId AND n.isRead = false")
    void markAllAsReadByReceiverId(@Param("receiverId") Long receiverId);

    // 읽음 처리된 지 특정 시점이 지난 오래된 알림 삭제
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt <= :targetDate")
    int deleteOldReadNotifications(@Param("targetDate") LocalDateTime targetDate);

    // 여러 알림을 한 번에 삭제 (벌크 연산으로 N+1 방지)
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.notificationId IN :notificationIds AND n.receiver.userId = :userId")
    void deleteByNotificationIdInAndReceiver_UserId(@Param("notificationIds") List<Long> notificationIds, @Param("userId") Long userId);
}
