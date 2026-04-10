package learntime.backend.domain.user.repository;

import learntime.backend.domain.user.model.PromptQuotas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PromptQuotaRepository extends JpaRepository<PromptQuotas, Long>  {

    // Util에서 넘겨준 maxQuota 기준으로 (초기화 후 1회 차감) 반영
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PromptQuotas q " +
            // 남은 횟수에 따라 소진 시간 설정/초기화
            "SET q.exhaustedAt = CASE " +
            "       WHEN q.remainingCount = 1 THEN CURRENT_TIMESTAMP " + // 남은 횟수가 1이면 이번 요청으로 0이 되므로 "지금 소진됨"
            "       WHEN q.remainingCount = 0 AND q.exhaustedAt <= :threshold THEN NULL " + // 이미 0이고, threshold(리셋 기준 시간)가 지났으면 "재충전 상태"
            "       ELSE q.exhaustedAt END, " + // 그 외에는 기존 값 유지
                // quota 초기화 또는 1 감소
            "    q.remainingCount = CASE " +
            "       WHEN q.remainingCount = 0 AND q.exhaustedAt <= :threshold THEN :maxQuota - 1 " +
            "       ELSE q.remainingCount - 1 END " +
            "WHERE q.userId = :userId " +
            // 남은 quota가 있거나, 리셋 가능한 경우만 실행
            "AND (q.remainingCount > 0 OR (q.remainingCount = 0 AND q.exhaustedAt <= :threshold))")
    int decreaseOrResetAtomic(
            @Param("userId") Long userId,
            @Param("threshold") LocalDateTime threshold,
            @Param("maxQuota") int maxQuota // 인자로 제한량 받음
    );

    // 보상 트랜잭션: 외부 API 실패 시 차감 롤백 및 소진 상태 해제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PromptQuotas q " +
            "SET q.remainingCount = q.remainingCount + 1, " +
            "    q.exhaustedAt = NULL " +
            "WHERE q.userId = :userId")
    int increaseCountAtomic(@Param("userId") Long userId);
}
