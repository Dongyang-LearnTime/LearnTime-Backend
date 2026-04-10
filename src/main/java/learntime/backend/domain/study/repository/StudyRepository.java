package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Study s SET s.isDeleted = true WHERE s.user.userId = :userId")
    void softDeleteAllByUserId(@Param("userId") Long userId);

    // ==================== [Hard Delete] ====================

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM Study s
        WHERE s.studyId = :studyId
        """)
    int hardDeleteById(@Param("studyId") Long studyId);

    // 다건(벌크) Hard Delete 최적화
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM study WHERE study_id IN (:studyIds)", nativeQuery = true)
    void deleteHardAllByIds(@Param("studyIds") List<Long> studyIds);
}
