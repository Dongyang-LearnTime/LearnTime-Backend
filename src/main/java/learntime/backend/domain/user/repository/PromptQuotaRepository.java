package learntime.backend.domain.user.repository;

import learntime.backend.domain.user.model.PromptQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptQuotaRepository extends JpaRepository<PromptQuota, Long>  {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PromptQuota q SET q.remainingCount = q.remainingCount - 1 WHERE q.userId = :userId AND q.remainingCount > 0")
    int decreaseCountAtomic(@Param("userId") Long userId);
}
