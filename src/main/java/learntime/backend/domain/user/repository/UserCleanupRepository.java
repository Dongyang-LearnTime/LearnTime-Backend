package learntime.backend.domain.user.repository;

import java.time.LocalDateTime;

public interface UserCleanupRepository {
    int hardDeleteOldUsers(LocalDateTime thresholdDate);
}

