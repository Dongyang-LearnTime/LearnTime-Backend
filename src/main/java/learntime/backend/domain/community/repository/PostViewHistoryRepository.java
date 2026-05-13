package learntime.backend.domain.community.repository;

import learntime.backend.domain.community.model.PostViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PostViewHistoryRepository extends JpaRepository<PostViewHistory, Long> {

    // 최근 조회 여부 확인
    boolean existsByPost_PostIdAndIpAddressAndCreatedAtAfter(
            Long postId,
            String ipAddress,
            LocalDateTime createdAt
    );

}