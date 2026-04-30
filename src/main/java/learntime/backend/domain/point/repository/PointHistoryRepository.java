package learntime.backend.domain.point.repository;

import learntime.backend.domain.point.model.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long>  {

    // 유저 Hard Delete 시 내역 벌크 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PointHistory p WHERE p.user.userId = :userId")
    void deleteBulkByUserId(@Param("userId") Long userId);

}
