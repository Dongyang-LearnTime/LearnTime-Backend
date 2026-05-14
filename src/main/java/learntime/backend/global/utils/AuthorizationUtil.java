package learntime.backend.global.utils;

import learntime.backend.global.dto.CustomUserDetails;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.exception.BusinessException;

public class AuthorizationUtil {

    private AuthorizationUtil() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    // DB에서 조회한 자원의 소유자 ID와 현재 인증된 사용자의 ID를 비교하여, 본인 여부 조회
    public static void verifyOwnership(Long currentUserId, Long ownerId) {
        if (currentUserId == null || ownerId == null) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED_ACCESS);
        }
        if (!currentUserId.equals(ownerId)) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED_ACCESS);
        }
    }
}
