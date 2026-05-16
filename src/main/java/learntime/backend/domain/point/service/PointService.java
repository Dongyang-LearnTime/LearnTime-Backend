package learntime.backend.domain.point.service;

import learntime.backend.domain.point.dto.PointRankingResponseDTO;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {
    private final UserRepository userRepository;

    public Page<PointRankingResponseDTO> getPointRanking(Pageable pageable) {
        Page<User> userPage = userRepository.findAllByOrderByPointDesc(pageable);

        int startRank = (int) pageable.getOffset() + 1;

        return userPage.map(user -> {
            int currentRank = startRank + (userPage.getContent().indexOf(user));
            return PointRankingResponseDTO.from(user, currentRank);
        });
    }
}
