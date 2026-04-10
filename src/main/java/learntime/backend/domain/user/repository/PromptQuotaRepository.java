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

    /**
     * [프롬프트 사용 시 할당량 차감 or 리셋]
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE PromptQuotas q
        SET 
            q.exhaustedAt = CASE
                WHEN q.remainingCount = 1 THEN :now
                WHEN q.remainingCount = 0 AND (q.exhaustedAt IS NULL OR q.exhaustedAt <= :threshold) THEN NULL
                ELSE q.exhaustedAt
            END,
            q.remainingCount = CASE
                WHEN q.remainingCount = 0 AND (q.exhaustedAt IS NULL OR q.exhaustedAt <= :threshold) 
                    THEN :maxQuota - 1
                ELSE q.remainingCount - 1
            END
        WHERE q.userId = :userId
          AND (
                q.remainingCount > 0 
                OR (q.remainingCount = 0 AND (q.exhaustedAt IS NULL OR q.exhaustedAt <= :threshold))
          )
    """)
    int decreaseOrResetAtomic(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold,
            @Param("maxQuota") int maxQuota
    );

    /**
     * [외부 API 실패 시 할당량 복구]
     * MySQL Left-to-Right Evaluation 방지: exhaustedAt을 먼저 갱신하여 remainingCount의 이전 값을 기반으로 안전하게 평가
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE PromptQuotas q
        SET q.exhaustedAt = CASE 
                WHEN q.remainingCount + 1 >= :maxQuota THEN NULL 
                ELSE q.exhaustedAt 
            END,
            q.remainingCount = q.remainingCount + 1
        WHERE q.userId = :userId AND q.remainingCount < :maxQuota
    """)
    int increaseCountAtomic(
            @Param("userId") Long userId,
            @Param("maxQuota") int maxQuota
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PromptQuotas p SET p.isDeleted = true WHERE p.userId = :userId")
    void softDeleteByUserId(@Param("userId") Long userId);
}