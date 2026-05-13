package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Modifying(clearAutomatically = true) // 쿼리 실행 후 영속성 컨텍스트 초기화
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    @Query(value = "SELECT file_url FROM post_image WHERE post_id IN (SELECT post_id FROM post WHERE deleted_at <= :threshold)", nativeQuery = true)
    List<String> findDeletedPostImageUrlsBefore(@Param("threshold") LocalDateTime threshold);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM post_view_history WHERE post_id IN (SELECT post_id FROM post WHERE deleted_at <= :threshold)", nativeQuery = true)
    void hardDeletePostViewHistoryByDeletedPostThreshold(@Param("threshold") LocalDateTime threshold);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM post_like WHERE post_id IN (SELECT post_id FROM post WHERE deleted_at <= :threshold)", nativeQuery = true)
    void hardDeletePostLikeByDeletedPostThreshold(@Param("threshold") LocalDateTime threshold);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM post_image WHERE post_id IN (SELECT post_id FROM post WHERE deleted_at <= :threshold)", nativeQuery = true)
    void hardDeletePostImageByDeletedPostThreshold(@Param("threshold") LocalDateTime threshold);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM comment WHERE post_id IN (SELECT post_id FROM post WHERE deleted_at <= :threshold)", nativeQuery = true)
    void hardDeleteCommentByDeletedPostThreshold(@Param("threshold") LocalDateTime threshold);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM post WHERE deleted_at <= :threshold", nativeQuery = true)
    int hardDeletePostByThreshold(@Param("threshold") LocalDateTime threshold);
}

