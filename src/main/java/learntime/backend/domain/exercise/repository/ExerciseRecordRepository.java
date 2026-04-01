package learntime.backend.domain.exercise.repository;

import learntime.backend.domain.exercise.model.ExerciseRecord;
import learntime.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, Long> {

    // 특정 사용자의 최근 7일 운동 내역 조회
    List<ExerciseRecord> findAllByUserAndCreateAtBetweenOrderByCreateAtAsc(
            User user, LocalDateTime start, LocalDateTime end);

    // 자식 테이블(exercise_parts) 선행 삭제
    @Modifying
    @Query(value = "DELETE FROM exercise_parts WHERE exercise_record_id IN " +
            "(SELECT id FROM exercise_record WHERE user_id = :userId)",
            nativeQuery = true)
    void deleteBodyPartsByUserId(@Param("userId") Long userId);

    // 부모 테이블(exercise_record) 데이터 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ExerciseRecord e WHERE e.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

}
