package learntime.backend.domain.community.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.model.PostViewHistory;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.community.repository.PostViewHistoryRepository;
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

    // 동시 다발적인 동일 IP 요청(따닥 현상)을 방지하기 위한 메모리 락 (1분간 유지)
    private final Cache<String, Boolean> viewLockCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    @Async
    // CommunityFacade.getPostDetails에 트랜잭션이 없으므로 fallbackExecution = true를 켜서 
    // 진행 중인 트랜잭션이 없어도 즉시 실행되도록 설정합니다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePostViewEvent(PostViewEventDTO event) {
        Long postId = event.postId();
        String ipAddress = event.ipAddress();
        String lockKey = postId + ":" + ipAddress;

        try {
            // 메모리 레벨에서 중복 요청 1차 차단 (Race Condition 방지)
            if (viewLockCache.getIfPresent(lockKey) != null) {
                return; 
            }
            viewLockCache.put(lockKey, true);

            // DB 레벨에서 최근 조회 여부 확인 (현재 기준: 24시간 = 1440분)
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            boolean recentlyViewed = postViewHistoryRepository.existsByPost_PostIdAndIpAddressAndCreatedAtAfter(postId, ipAddress, yesterday);

            if (!recentlyViewed) {
                // 프록시 객체를 사용하여 불필요한 SELECT 쿼리 방지
                Post post = postRepository.getReferenceById(postId);
                postViewHistoryRepository.save(new PostViewHistory(post, ipAddress));
                postRepository.incrementViewCount(postId);
            }
        } catch (Exception e) {
            // 예외 발생 시 다른 요청 처리를 위해 락 해제
            viewLockCache.invalidate(lockKey);
            log.warn("게시글 조회수 증가 처리 중 오류가 발생했습니다. postId: {}, ip: {}", postId, ipAddress, e);
        }
    }
}
