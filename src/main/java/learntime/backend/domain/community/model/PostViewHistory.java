package learntime.backend.domain.community.model;

import jakarta.persistence.*;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;

@Entity
@Table(
        name = "post_view_history",
        indexes = {
                @Index(
                        name = "idx_post_view_post_ip",
                        columnList = "post_id, ip_address"
                ),

                @Index(
                        name = "idx_post_view_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostViewHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postViewHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    public PostViewHistory(Post post, String ipAddress) {
        this.post = post;
        this.ipAddress = ipAddress;
    }

}