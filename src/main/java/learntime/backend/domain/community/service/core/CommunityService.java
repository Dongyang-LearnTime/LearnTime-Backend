package learntime.backend.domain.community.service.core;

import learntime.backend.domain.community.converter.CommunityConverter;
import learntime.backend.domain.community.dto.response.PointRankingResponseDTO;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {
    private final UserRepository userRepository;

    public Page<PointRankingResponseDTO> getPointRanking(Pageable pageable) {
        Pageable rankingPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("point"),
                        Sort.Order.asc("userId")
                )
        );
        Page<User> userPage = userRepository.findAll(rankingPageable);
        int startRank = (int) rankingPageable.getOffset() + 1;

        return userPage.map(user -> {
            int currentRank = startRank + userPage.getContent().indexOf(user);

            return CommunityConverter.toPointRankingResponseDTO(user, currentRank);
        });
    }
}
