package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.Comment;
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
    List<Comment> findByPostIdWithUserWithCursor(@Param("postId") Long postId, @Param("lastCommentId") Long lastCommentId, org.springframework.data.domain.Pageable pageable);

    /** 첫 페이지 댓글 목록을 cursor 기반으로 조회.*/
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user " +
            "WHERE c.post.postId = :postId " +
            "ORDER BY c.commentId DESC")
    List<Comment> findFirstPageByPostIdWithUser(@Param("postId") Long postId, org.springframework.data.domain.Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM comment WHERE deleted_at <= :threshold", nativeQuery = true)
    int hardDeleteCommentByThreshold(@Param("threshold") LocalDateTime threshold);
}

