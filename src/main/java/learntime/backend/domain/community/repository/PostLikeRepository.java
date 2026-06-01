package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    
    /** 특정 사용자가 특정 게시글에 좋아요(추천)를 눌렀는지 여부를 확인함. */
    boolean existsByPost_PostIdAndUser_UserId(Long postId, Long userId);

    void deleteByPost_PostIdAndUser_UserId(Long postId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PostLike pl WHERE pl.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    /** 특정 사용자의 게시글에 달린 좋아요 총합 */
    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM Post p WHERE p.user.userId = :userId")
    long sumLikeCountByAuthorId(@Param("userId") Long userId);
}
