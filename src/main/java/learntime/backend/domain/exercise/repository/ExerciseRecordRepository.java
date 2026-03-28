package learntime.backend.domain.exercise.repository;

import learntime.backend.domain.exercise.entity.ExerciseRecord;
import learntime.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, Long> {

    // 특정 사용자의 최근 7일 운동 내역 조회
    List<ExerciseRecord> findAllByUserAndCreateAtBetweenOrderByCreateAtAsc(
            User user, LocalDateTime start, LocalDateTime end);
}
