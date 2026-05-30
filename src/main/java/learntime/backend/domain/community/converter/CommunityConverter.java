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

    public static PointRankingResponseDTO toPointRankingResponseDTO(
            User user,
            int rank,
            Boolean hasBlocked
    ) {
        String tireName = PointMilestone.getTier(user.getPoint()).getTierName();

        return PointRankingResponseDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .point(user.getPoint())
                .tierName(tireName)
                .rank(rank)
                .hasBlocked(hasBlocked)
                .build();
    }

}
