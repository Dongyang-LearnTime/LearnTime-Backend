package learntime.backend.domain.admin.converter;

import learntime.backend.domain.admin.dto.response.AdminUserDetailResponseDTO;
import learntime.backend.domain.admin.dto.response.AdminUserListResponseDTO;
import learntime.backend.domain.admin.dto.response.SiteStatsResponseDTO;
import learntime.backend.domain.user.model.PromptQuotas;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class AdminConverter {

    public AdminConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }


    public static SiteStatsResponseDTO toSiteStatsResponseDTO(
            long totalUsers, long todayNewUsers,
            long totalPosts, long todayNewPosts,
            long totalComments, long todayNewComments) {
        
        return new SiteStatsResponseDTO(
                totalUsers, todayNewUsers,
                totalPosts, todayNewPosts,
                totalComments, todayNewComments
        );
    }

    public static AdminUserListResponseDTO toAdminUserListResponseDTO(User user) {
        return new AdminUserListResponseDTO(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getSocialProvider(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    public static AdminUserDetailResponseDTO toAdminUserDetailResponseDTO(User user, PromptQuotas quotas) {
        Integer remainingCount = (quotas != null) ? quotas.getRemainingCount() : 0;
        
        return new AdminUserDetailResponseDTO(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPoint(),
                user.getSocialProvider(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getFailedAttempts(),
                user.isAccountLocked(),
                user.getLockedAt(),
                remainingCount
        );
    }
}
