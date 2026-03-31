package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Study s WHERE s.user.userId = :userId")
    void deleteAllByUserIdInBatch(@Param("userId") Long userId);
}
