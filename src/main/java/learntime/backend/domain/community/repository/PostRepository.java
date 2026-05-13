package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Modifying(clearAutomatically = true) // 쿼리 실행 후 영속성 컨텍스트 초기화
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    int incrementViewCount(@Param("postId") Long postId);
}
