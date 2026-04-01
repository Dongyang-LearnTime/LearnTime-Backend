package learntime.backend.domain.exercise.repository;

import learntime.backend.domain.user.model.User;
import learntime.backend.domain.exercise.model.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {

    // 특정 사용자의 최근 7일 데이터를 생성일 순으로 조회 -> 몸무게 및 체지방량
    List<WeightRecord> findAllByUserAndCreateAtBetweenOrderByCreateAtAsc(
            User user, LocalDateTime start, LocalDateTime end);
}
