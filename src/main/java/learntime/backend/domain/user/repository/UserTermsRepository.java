package learntime.backend.domain.user.repository;

import learntime.backend.domain.user.model.UserTerms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsRepository extends JpaRepository<UserTerms, Long>  {
}
