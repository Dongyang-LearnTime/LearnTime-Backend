package learntime.backend.domain.calendar.repository;

import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarRecordRepository extends JpaRepository<CalendarRecord, Long> {

    // 특정 사용자의 특정 기간을 조회 (한달 혹은 일주일치 일정을 불러 올 때, 아래의 메서드 이용)
    List<CalendarRecord> findAllByUserAndTargetDateBetweenOrderByTargetDateAsc(
            User user, LocalDateTime start, LocalDateTime end);
}
