package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /** 게시글 목록을 DTO로 바로 조회하며, 페이징 처리를 최적화합니다. */
    @Query(value = "SELECT new learntime.backend.domain.community.dto.response.PostListResponseDTO(" +
            "p.postId, u.userId, u.name, p.title, p.viewCount, p.likeCount, " +
            "(SELECT COUNT(c) FROM Comment c WHERE c.post.postId = p.postId), " +
            "p.createdAt) " +
            "FROM Post p " +
            "LEFT JOIN p.user u",
           countQuery = "SELECT COUNT(p) FROM Post p")
    Page<PostListResponseDTO> findAllPostsWithCommentCount(Pageable pageable);

    @Modifying(clearAutomatically = true) // 쿼리 실행 후 영속성 컨텍스트 초기화
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    @Query(value = "SELECT file_url FROM post_image " +
            "WHERE post_id IN " +
            "(SELECT post_id FROM post WHERE deleted_at <= :threshold)", nativeQuery = true)
    List<String> findDeletedPostImageUrlsBefore(@Param("threshold") LocalDateTime threshold);

    /** 게시글과 연관된 작성자(User), 스터디(Study)를 FETCH JOIN으로 한 번의 쿼리로 가져옴. (이미지는 별도 조회) */
    @Query("SELECT p " +
            "FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.study " +
            "WHERE p.postId = :postId")
    Optional<Post> findByIdWithDetails(@Param("postId") Long postId);


    // hard delete 벌크 삭제 용
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

