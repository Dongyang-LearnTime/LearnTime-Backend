package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.time.LocalDateTime;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    @Query(value = "SELECT file_url FROM post_image WHERE deleted_at <= :threshold", nativeQuery = true)
    List<String> findDeletedImageUrlsBefore(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query(value = "DELETE FROM post_image WHERE deleted_at <= :threshold", nativeQuery = true)
    int hardDeleteImagesBefore(@Param("threshold") LocalDateTime threshold);

    List<PostImage> findByPost_PostId(Long postId);

    @Query("SELECT pi.fileUrl FROM PostImage pi WHERE pi.post.postId = :postId")
    List<String> findFileUrlsByPostId(@Param("postId") Long postId);
}
