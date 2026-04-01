package learntime.backend.global.utils;

import learntime.backend.domain.user.repository.PromptQuotaRepository;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PromptQuotaUtil {

    private final PromptQuotaRepository promptQuotaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreasePromptQuota(Long userId) {
        int updatedRows = promptQuotaRepository.decreaseCountAtomic(userId);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.PROMPT_QUOTA_EXCEEDED);
        }
    }

    // 외부 API 호출 실패 시 횟수 롤백(보상 트랜잭션)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restorePromptQuota(Long userId) {
        promptQuotaRepository.increaseCountAtomic(userId);
    }

}

