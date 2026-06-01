package learntime.backend.domain.community.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.model.PostViewHistory;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.community.repository.PostViewHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventListener {

    private final PostRepository postRepository;
    private final PostViewHistoryRepository postViewHistoryRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Async
    // CommunityFacade.getPostDetails에 트랜잭션이 없으므로 fallbackExecution = true를 켜서 
    // 진행 중인 트랜잭션이 없어도 즉시 실행되도록 설정합니다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePostViewEvent(PostViewEventDTO event) {
        Long postId = event.postId();
        String ipAddress = event.ipAddress();
        String lockKey = "view_" + postId + "_" + ipAddress;

        try {
            // MySQL GET_LOCK 사용 (다중 서버 Race Condition 방지)
            Query lockQuery = entityManager.createNativeQuery("SELECT GET_LOCK(:key, 1)");
            lockQuery.setParameter("key", lockKey);
            Number lockResult = (Number) lockQuery.getSingleResult();

            if (lockResult != null && lockResult.intValue() == 1) {
                // DB 레벨에서 최근 조회 여부 확인 (현재 기준: 24시간 = 1440분)
                LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
                boolean recentlyViewed = postViewHistoryRepository.existsByPost_PostIdAndIpAddressAndCreatedAtAfter(postId, ipAddress, yesterday);

                if (!recentlyViewed) {
                    // 프록시 객체를 사용하여 불필요한 SELECT 쿼리 방지
                    Post post = postRepository.getReferenceById(postId);
                    postViewHistoryRepository.save(new PostViewHistory(post, ipAddress));
                    postRepository.incrementViewCount(postId);
                }
            }
        } catch (Exception e) {
            log.warn("게시글 조회수 증가 처리 중 오류가 발생했습니다. postId: {}, ip: {}", postId, ipAddress, e);
        } finally {
            Query releaseQuery = entityManager.createNativeQuery("SELECT RELEASE_LOCK(:key)");
            releaseQuery.setParameter("key", lockKey);
            releaseQuery.getSingleResult();
        }
    }
}
