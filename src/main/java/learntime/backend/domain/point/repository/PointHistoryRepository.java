package learntime.backend.domain.point.repository;

import learntime.backend.domain.point.model.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long>  {
}
