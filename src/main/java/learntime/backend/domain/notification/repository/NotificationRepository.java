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
// 사용자 알림의 조회와 읽음 대상 식별을 담당하는 Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 내 알림 목록을 최신순으로 조회
    List<Notification> findAllByReceiver_UserIdOrderByCreatedAtDesc(Long receiverId);

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
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.updatedAt <= :targetDate")
    int deleteOldReadNotifications(@Param("targetDate") LocalDateTime targetDate);
}
