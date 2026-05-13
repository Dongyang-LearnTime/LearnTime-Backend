package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM comment WHERE deleted_at <= :threshold", nativeQuery = true)
    int hardDeleteCommentByThreshold(@Param("threshold") LocalDateTime threshold);
}

