package learntime.backend.domain.community.scheduler;

import learntime.backend.domain.community.service.CommunityCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityScheduler {

    private final CommunityCleanupService communityCleanupService;

    // 매일 새벽 3시에 90일이 지난 커뮤니티 삭제 데이터 일괄 정리
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupDeletedCommunityData() {
        log.info("Executing scheduled task: cleanupDeletedCommunityData");
        try {
            communityCleanupService.hardDeleteOldPostsAndComments();
            log.info("Completed scheduled task: cleanupDeletedCommunityData successfully.");
        } catch (Exception e) {
            log.error("Failed to execute scheduled task: cleanupDeletedCommunityData", e);
        }
    }
}
