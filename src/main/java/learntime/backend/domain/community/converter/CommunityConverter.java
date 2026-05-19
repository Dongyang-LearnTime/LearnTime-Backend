package learntime.backend.domain.community.converter;

import learntime.backend.domain.community.dto.response.PointRankingResponseDTO;
import learntime.backend.domain.point.enums.PointMilestone;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class CommunityConverter {

    public CommunityConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static PointRankingResponseDTO toPointRankingResponseDTO(User user, int rank) {
        return new PointRankingResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getPoint(),
                PointMilestone.getTier(user.getPoint()).getTierName(),
                rank
        );
    }

}
