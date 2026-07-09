package learntime.backend.domain.admin.service;

import learntime.backend.domain.admin.converter.AdminConverter;
import learntime.backend.domain.admin.dto.response.SiteStatsResponseDTO;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSiteService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // 사이트 전반의 통계 현황 조회
    public SiteStatsResponseDTO getSiteStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        long totalUsers = userRepository.count();
        long todayNewUsers = userRepository.countByCreatedAtAfter(startOfToday);

        long totalPosts = postRepository.count();
        long todayNewPosts = postRepository.countByCreatedAtAfter(startOfToday);

        long totalComments = commentRepository.count();
        long todayNewComments = commentRepository.countByCreatedAtAfter(startOfToday);

        return AdminConverter.toSiteStatsResponseDTO(
                totalUsers, todayNewUsers,
                totalPosts, todayNewPosts,
                totalComments, todayNewComments
        );
    }
}
