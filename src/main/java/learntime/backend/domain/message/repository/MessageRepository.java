package learntime.backend.domain.message.repository;

import learntime.backend.domain.message.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query(value = "select m from Message m join fetch m.receiver where m.sender.userId = :senderId and m.senderDeleted = false",
            countQuery = "select count(m) from Message m where m.sender.userId = :senderId and m.senderDeleted = false")
    Page<Message> findSentMessages(@Param("senderId") Long senderId, Pageable pageable);

    @Query(value = "select m from Message m join fetch m.sender where m.receiver.userId = :receiverId and m.receiverDeleted = false",
            countQuery = "select count(m) from Message m where m.receiver.userId = :receiverId and m.receiverDeleted = false")
    Page<Message> findReceivedMessages(@Param("receiverId") Long receiverId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update Message m set m.readAt = :readAt " +
            "where m.messageId in :messageIds " +
            "and m.receiver.userId = :userId " +
            "and m.receiverDeleted = false " +
            "and m.readAt is null")
    int markAsRead(@Param("messageIds") java.util.List<Long> messageIds,
                   @Param("userId") Long userId,
                   @Param("readAt") java.time.LocalDateTime readAt);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("delete from Message m " +
            "where m.senderDeleted = true " +
            "and m.receiverDeleted = true " +
            "and m.readAt is not null " +
            "and m.readAt <= :threshold")
    int deleteExpiredMessages(@Param("threshold") java.time.LocalDateTime threshold);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update Message m set m.senderDeleted = true where m.sender.userId = :userId")
    void deleteSentMessagesByUserId(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update Message m set m.receiverDeleted = true where m.receiver.userId = :userId")
    void deleteReceivedMessagesByUserId(@Param("userId") Long userId);
}
