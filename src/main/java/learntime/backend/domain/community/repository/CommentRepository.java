package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 특정 게시글에 달린 댓글 목록을 cursor 기반으로 조회.*/
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user " +
            "WHERE c.post.postId = :postId AND c.commentId < :lastCommentId " +
            "ORDER BY c.commentId DESC")
    List<Comment> findByPostIdWithUserWithCursor(@Param("postId") Long postId, @Param("lastCommentId") Long lastCommentId, Pageable pageable);

    /** 첫 페이지 댓글 목록을 cursor 기반으로 조회.*/
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user " +
            "WHERE c.post.postId = :postId " +
            "ORDER BY c.commentId DESC")
    List<Comment> findFirstPageByPostIdWithUser(@Param("postId") Long postId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.user = null WHERE c.user.userId = :userId")
    void detachAuthorByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM comment WHERE deleted_at <= :threshold", nativeQuery = true)
    int hardDeleteCommentByThreshold(@Param("threshold") LocalDateTime threshold);

    /** 게시글 ID 목록에 따른 댓글 개수 일괄 조회 */
    @Query("SELECT c.post.postId, COUNT(c) FROM Comment c WHERE c.post.postId IN :postIds GROUP BY c.post.postId")
    List<Object[]> countCommentsByPostIds(@Param("postIds") List<Long> postIds);

    /** 내가 쓴 댓글 오프셋 페이징 조회 (게시글 fetch join) */
    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.post p WHERE c.user.userId = :userId ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.user.userId = :userId")
    Page<Comment> findMyComments(@Param("userId") Long userId, Pageable pageable);

    /** 내가 쓴 댓글 총 개수 */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    long countByCreatedAtAfter(LocalDateTime date);
}
