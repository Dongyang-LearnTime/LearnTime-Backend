package learntime.backend.domain.calendar.repository;

import learntime.backend.domain.calendar.model.Routine;
import learntime.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, Long> {

    @Query("SELECT DISTINCT r FROM Routine r LEFT JOIN FETCH r.daysOfWeek WHERE r.user = :user")
    List<Routine> findAllByUser(@Param("user") User user);

    @Query("SELECT r FROM Routine r LEFT JOIN FETCH r.daysOfWeek WHERE r.endDate IS NULL OR r.endDate >= :today")
    List<Routine> findAllActiveRoutines(@Param("today") LocalDate today);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM routine_days WHERE routine_id IN (SELECT r.routine_id FROM routine r WHERE r.user_id = :userId)", nativeQuery = true)
    void deleteDaysByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Routine r WHERE r.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
