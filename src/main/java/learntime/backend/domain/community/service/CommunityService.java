package learntime.backend.domain.community.service;

import learntime.backend.domain.community.converter.CommunityConverter;
import learntime.backend.domain.community.dto.response.PointRankingResponseDTO;
import learntime.backend.domain.relationship.repository.UserBlockRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;

    public Page<PointRankingResponseDTO> getPointRanking(Pageable pageable, Long userId) {
        Pageable rankingPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("point"),
                        Sort.Order.asc("userId")
                )
        );
        Page<User> userPage = userRepository.findAll(rankingPageable);

        Set<Long> blockedIds = userId == null
                ? Collections.emptySet()
                : userBlockRepository.findBlockedUserIds(userId);

        int startRank = (int) rankingPageable.getOffset() + 1;

        return userPage.map(user -> {
            int currentRank = startRank + userPage.getContent().indexOf(user);
            Boolean hasBlocked = userId == null
                    ? null
                    : blockedIds.contains(user.getUserId());
            return CommunityConverter.toPointRankingResponseDTO(
                    user,
                    currentRank,
                    hasBlocked
            );
        });

    }
}
