package learntime.backend.domain.study.repository;

import jakarta.persistence.LockModeType;
import learntime.backend.domain.study.model.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {

    // 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Study s
        WHERE s.studyId = :studyId
    """)
    Optional<Study> findByIdWithPessimisticLock(Long studyId);
}
