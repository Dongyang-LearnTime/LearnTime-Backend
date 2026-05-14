package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    List<PostImage> findByPost_PostId(Long postId);

    @Query("SELECT pi.fileUrl FROM PostImage pi WHERE pi.post.postId = :postId")
    List<String> findFileUrlsByPostId(@Param("postId") Long postId);
}
