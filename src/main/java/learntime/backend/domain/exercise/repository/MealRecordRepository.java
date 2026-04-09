package learntime.backend.domain.exercise.repository;

import learntime.backend.domain.exercise.model.MealRecord;
import learntime.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    List<MealRecord> findAllByUserAndCreateAtBetweenOrderByCreateAtAsc(
            User user, LocalDateTime start, LocalDateTime end);
}
