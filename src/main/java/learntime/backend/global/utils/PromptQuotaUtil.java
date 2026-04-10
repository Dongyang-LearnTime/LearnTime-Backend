package learntime.backend.global.utils;

import learntime.backend.domain.user.repository.PromptQuotaRepository;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PromptQuotaUtil {

    private final PromptQuotaRepository promptQuotaRepository;

    @Value("${gemini.api.max-quota}") // 프롬프트 할당량
    private int maxQuota;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreasePromptQuota(Long userId) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        int updatedRows = promptQuotaRepository.decreaseOrResetAtomic(userId, threshold, maxQuota);

        // 조건(잔여량 0 & 24시간 안지남)에 걸려 업데이트된 row가 없다면 예외 발생
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.PROMPT_QUOTA_EXCEEDED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restorePromptQuota(Long userId) {
        promptQuotaRepository.increaseCountAtomic(userId);
    }

}

