package learntime.backend.domain.notification.model;

import jakarta.persistence.*;
import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "notification")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    /** 알림 고유 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    /** 알림을 받는 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User receiver;

    /** 알림 종류 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /** 알림 제목 */
    @Column(nullable = false, length = 100)
    private String title;

    /** 알림 내용 */
    @Column(nullable = false, length = 255)
    private String message;

    /** 알림과 연결된 대상 ID */
    @Column(nullable = false, name = "reference_id")
    private Long referenceId;

    /** 알림과 연결된 대상 종류 */
    @Column(nullable = false, name = "reference_type", length = 50)
    private String referenceType;

    /** 사용자가 알림을 읽었는지 여부 */
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    public void markAsRead() {
        this.isRead = true;
    }
}
