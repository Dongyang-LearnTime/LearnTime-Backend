package learntime.backend.global.utils;

import learntime.backend.domain.relationship.error.code.RelationShipCode;
import learntime.backend.domain.relationship.error.exception.RelationShipException;
import learntime.backend.domain.relationship.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserBlockUtil {

    private final UserBlockRepository userBlockRepository;

    // 대상 사용자가 로그인 사용자를 차단했는지 검증
    public void validateNotBlockedByUser(Long loginUserId, Long targetUserId) {
        if (userBlockRepository.existsByBlocker_UserIdAndBlocked_UserId(targetUserId, loginUserId)) {
            throw new RelationShipException(RelationShipCode.BLOCKED_BY_USER);
        }
    }

}