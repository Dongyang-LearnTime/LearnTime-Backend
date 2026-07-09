package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /** 게시글 목록을 페이징 조회합니다. (작성자 fetch join) */
    @Query(value = "SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user u " +
            "WHERE p.isNotice = false",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.isNotice = false")
    Page<Post> findAllPosts(Pageable pageable);

    /** 제목 또는 내용으로 게시글을 페이징 조회합니다. (작성자 fetch join) */
    @Query(
            value = """
                SELECT p FROM Post p
                JOIN FETCH p.user u
                WHERE (
                    p.title LIKE CONCAT(:keyword, '%')
                    OR p.content LIKE CONCAT(:keyword, '%')
                )
                AND p.isNotice = false
                """,
            countQuery = """
                SELECT COUNT(p) FROM Post p
                WHERE (
                    p.title LIKE CONCAT(:keyword, '%')
                    OR p.content LIKE CONCAT(:keyword, '%')
                )
                AND p.isNotice = false
                """
    )
    Page<Post> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT p FROM Post p
                JOIN FETCH p.user u
                WHERE u.name LIKE CONCAT(:name, '%')
                AND p.isNotice = false
                """,
            countQuery = """
                SELECT COUNT(p) FROM Post p
                JOIN p.user u
                WHERE u.name LIKE CONCAT(:name, '%')
                AND p.isNotice = false
                """
    )
    Page<Post> searchByAuthorName(@Param("name") String name, Pageable pageable);


    /** 주간 인기글 목록을 조회합니다. (작성자 fetch join) */
    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user u " +
            "WHERE p.createdAt >= :startDate " +
            "AND p.isNotice = false " +
            "ORDER BY p.likeCount DESC")
    List<Post> findWeeklyPopularPosts(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    /** 공지사항 목록을 최신순으로 조회합니다. (작성자 fetch join) */
    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user u " +
            "WHERE p.isNotice = true " +
            "ORDER BY p.createdAt DESC")
    List<Post> findNoticePosts();

    /** 특정 사용자의 최근 게시글 목록을 조회합니다. (작성자 fetch join) */
    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user u " +
            "WHERE u.userId = :userId AND p.isNotice = false " +
            "ORDER BY p.createdAt DESC")
    List<Post> findRecentPostsByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 내가 쓴 게시글 오프셋 페이징 조회 */
    @Query(value = "SELECT p FROM Post p LEFT JOIN FETCH p.user u WHERE p.user.userId = :userId AND p.isNotice = false ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user.userId = :userId AND p.isNotice = false")
    Page<Post> findMyPosts(@Param("userId") Long userId, Pageable pageable);

    /** 내가 쓴 게시글 총 개수 */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.userId = :userId AND p.isNotice = false")
    long countByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true) // 쿼리 실행 후 영속성 컨텍스트 초기화
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Post p
            SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END
            WHERE p.postId IN (
                  SELECT pl.post.postId
                  FROM PostLike pl
                  WHERE pl.user.userId = :userId
              )
            """)
    void decrementLikeCountForUserLikes(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.user = null WHERE p.user.userId = :userId")
    void detachAuthorByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT file_url FROM post_image " +
            "WHERE post_id IN " +
            "(SELECT post_id FROM post WHERE deleted_at <= :threshold)", nativeQuery = true)
    List<String> findDeletedPostImageUrlsBefore(@Param("threshold") LocalDateTime threshold);

    /** 게시글과 연관된 작성자(User)를 FETCH JOIN으로 한 번의 쿼리로 가져옴. (이미지는 별도 조회) */
    @Query("SELECT p " +
            "FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.postId = :postId")
    Optional<Post> findByIdWithDetails(@Param("postId") Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.postId = :postId")
    Optional<Post> findByIdForUpdate(@Param("postId") Long postId);


    /** hard delete 벌크 삭제 용 */
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

    long countByCreatedAtAfter(LocalDateTime date);
}

