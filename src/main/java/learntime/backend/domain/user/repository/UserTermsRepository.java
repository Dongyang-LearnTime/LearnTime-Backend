package learntime.backend.domain.user.repository;

import learntime.backend.domain.user.model.UserTerms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTermsRepository extends JpaRepository<UserTerms, Long>  {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserTerms ut WHERE ut.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
