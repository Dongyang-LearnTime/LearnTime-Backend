package learntime.backend.global.utils;

import learntime.backend.domain.user.model.PromptQuotas;
import learntime.backend.domain.user.repository.PromptQuotaRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.PromptQuotaException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class PromptQuotaUtil {

    private final PromptQuotaRepository promptQuotaRepository;

    @Value("${gemini.api.max-quota}")
    private int maxQuota;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public void decreasePromptQuota(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusHours(24);

        int updatedRows = promptQuotaRepository.decreaseOrResetAtomic(
                userId,
                now,
                threshold,
                maxQuota
        );

        if (updatedRows == 0) {
            PromptQuotas quota = promptQuotaRepository.findById(userId)
                    .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

            LocalDateTime exhaustedAt = quota.getExhaustedAt();
            LocalDateTime availableAt = (exhaustedAt != null)
                    ? exhaustedAt.plusHours(24)
                    : now.plusHours(24);

            throw new PromptQuotaException(
                    ErrorCode.PROMPT_QUOTA_EXCEEDED,
                    "현재 AI 사용량을 초과했습니다. "
                            + availableAt.format(TIME_FORMATTER)
                            + " 이후에 다시 시도해주세요."
            );
        }
    }

    @Transactional
    public void restorePromptQuota(Long userId) {
        promptQuotaRepository.increaseCountAtomic(userId, maxQuota);
    }

}