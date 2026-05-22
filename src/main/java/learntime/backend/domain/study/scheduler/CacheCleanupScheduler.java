package learntime.backend.domain.study.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheCleanupScheduler {

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul") // 매일 자정 실행
    @Caching(evict = {
            @CacheEvict(value = "recentWeekStudyIndicator", allEntries = true),
            @CacheEvict(value = "studyDailyPlans", allEntries = true)
    })
    public void evictAllCaches() {
        log.info("[캐시 초기화] 스케줄러(자정) 캐시 일괄 삭제 완료 (recentWeekStudyIndicator, studyDailyPlans)");
    }
}
