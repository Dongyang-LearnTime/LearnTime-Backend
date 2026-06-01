package learntime.backend.domain.exercise.repository;

import learntime.backend.domain.user.model.User;
import learntime.backend.domain.exercise.model.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {

    // 특정 사용자의 최근 7일 데이터를 생성일 순으로 조회 -> 몸무게 및 체지방량
    List<WeightRecord> findAllByUserAndCreatedAtBetweenOrderByCreatedAtAsc(
            User user, LocalDateTime start, LocalDateTime end);

    // 사용자의 전체 데이터를 최신순으로 조회
    List<WeightRecord> findAllByUserOrderByCreatedAtDesc(User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM WeightRecord w WHERE w.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
