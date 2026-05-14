package learntime.backend.domain.community.scheduler;

import learntime.backend.domain.community.service.core.CommunityCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityScheduler {

    private final CommunityCleanupService communityCleanupService;

    // 서버 작동 후 바로 시작
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        log.info("[커뮤니티 정리 시작] 서버 시작 후 커뮤니티 삭제 데이터 정리 작업 실행");

        long startTime = System.currentTimeMillis();

        try {
            communityCleanupService.hardDeleteOldPostsAndComments();
            long endTime = System.currentTimeMillis();

            log.info(
                    "[커뮤니티 정리 완료] 서버 시작 정리 작업 성공 - 소요 시간: {}ms",
                    (endTime - startTime)
            );

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error(
                    "[커뮤니티 정리 실패] 서버 시작 정리 작업 실패 - 소요 시간: {}ms",
                    (endTime - startTime),
                    e
            );
        }
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void cleanupDeletedCommunityData() {

        log.info("[커뮤니티 스케줄러 시작] 90일 지난 삭제 데이터 정리 작업 시작");
        long startTime = System.currentTimeMillis();
        try {
            communityCleanupService.hardDeleteOldPostsAndComments();
            long endTime = System.currentTimeMillis();
            log.info(
                    "[커뮤니티 스케줄러 완료] 삭제 데이터 정리 성공 - 소요 시간: {}ms",
                    (endTime - startTime)
            );

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error(
                    "[커뮤니티 스케줄러 실패] 삭제 데이터 정리 실패 - 소요 시간: {}ms",
                    (endTime - startTime),
                    e
            );
        }
    }
}
