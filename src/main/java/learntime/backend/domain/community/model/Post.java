package learntime.backend.domain.community.model;

import jakarta.persistence.*;
import learntime.backend.domain.user.model.User;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table
// soft delete
@SQLDelete(sql = "UPDATE post SET deleted_at = CURRENT_TIMESTAMP WHERE post_id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends CommunityBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;


    // SET NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String studySnapshot;

    @Column
    private Long studyId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @Builder.Default
    @Column(nullable = false)
    private Integer likeCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer viewCount = 0; // 조회수

    @Builder.Default
    @Column(nullable = false)
    private boolean isNotice = false; // 공지사항 여부

    // 댓글
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // 게시글 이미지
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> images = new ArrayList<>();

    // 좋아요 누른 사용자 목록
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLikes = new ArrayList<>();

    // 최근 조회한 사용자 IP
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostViewHistory> postViewHistories = new ArrayList<>();

    // --- 연관관계 편의 메서드 --- //

    // 이미지 추가
    public void addImage(PostImage image) {
        images.add(image);
        image.setPost(this);
    }

    // 좋아요 증가
    public void incrementLikeCount() {
        this.likeCount++;
    }

    // 좋아요 감소
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    // 조회수 증가
    public void incrementViewCount() { this.viewCount++; }

    // 게시글 수정
    public void updatePost(String title, String content, Long studyId, String studySnapshot) {
        this.title = title;
        this.content = content;
        this.studyId = studyId;
        this.studySnapshot = studySnapshot;
    }
}
