package learntime.backend.domain.message.model;

import jakarta.persistence.*;
import learntime.backend.domain.user.model.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "message",
        indexes = {
                @Index(
                        name = "idx_receiver_box",
                        columnList = "receiver_id, receiver_deleted, sent_at"
                ),
                @Index(
                        name = "idx_sender_box",
                        columnList = "sender_id, sender_deleted, sent_at"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @Column(nullable = false, length = 1000)
    private String content;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column
    private LocalDateTime readAt;

    @Column(nullable = false)
    private boolean senderDeleted = false;

    @Column(nullable = false)
    private boolean receiverDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Builder
    public Message(String content, User sender, User receiver) {
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
    }

    public void readMessage() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public void deleteBySender() {
        this.senderDeleted = true;
    }

    public void deleteByReceiver() {
        this.receiverDeleted = true;
    }

    public boolean isCompletelyDeleted() {
        return senderDeleted && receiverDeleted;
    }
}