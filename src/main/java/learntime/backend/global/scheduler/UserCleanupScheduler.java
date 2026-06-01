package learntime.backend.global.scheduler;

import learntime.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserRepository userRepository;

    // 매일 새벽 4시마다 DB 테이블 정리 수행 (보관 기간 180일이 지난 탈퇴 사용자 Hard Delete)
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void processHardDelete() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(180);
        log.info("보관 기간(180일)이 지난 탈퇴 사용자 Hard Delete 작업 시작");
        
        int deletedCount = userRepository.hardDeleteOldUsers(thresholdDate);
        
        log.info("Hard Delete 완료: 총 {} 건 삭제", deletedCount);
    }
}
