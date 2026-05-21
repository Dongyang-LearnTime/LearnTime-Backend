package learntime.backend.domain.exercise.repository;

import learntime.backend.domain.exercise.model.MealRecord;
import learntime.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    List<MealRecord> findAllByUserAndCreatedAtBetweenOrderByCreatedAtAsc(
            User user, LocalDateTime start, LocalDateTime end);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MealRecord m WHERE m.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
