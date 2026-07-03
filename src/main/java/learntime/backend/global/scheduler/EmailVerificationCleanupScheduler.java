package learntime.backend.global.scheduler;

import learntime.backend.domain.user.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {

    private final EmailVerificationRepository emailVerificationRepository;

    // 매일 새벽 5시에 실행 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void cleanupOldEmailVerifications() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        int deletedCount = emailVerificationRepository.deleteByCreatedAtBefore(oneMonthAgo);
        
        log.info("오래된 이메일 인증 레코드 정리 완료: {} 건 삭제됨 (기준일: {})", deletedCount, oneMonthAgo);
    }
}
