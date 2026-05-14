package learntime.backend.domain.community.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table
// soft delete
@SQLDelete(sql = "UPDATE post_image SET deleted_at = CURRENT_TIMESTAMP WHERE post_image_id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends CommunityBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postImageId;

    @Column(nullable = false, length = 512)
    private String fileUrl;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

}