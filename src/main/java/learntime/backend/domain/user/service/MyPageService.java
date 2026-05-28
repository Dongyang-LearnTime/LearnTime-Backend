package learntime.backend.domain.user.service;

import learntime.backend.domain.community.converter.PostConverter;
import learntime.backend.domain.community.dto.response.MyCommentListResponseDTO;
import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import learntime.backend.domain.community.model.Comment;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostLikeRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.user.converter.UserConverter;
import learntime.backend.domain.user.dto.response.MyPageResponseDTO;
import learntime.backend.domain.user.dto.response.MyPageSummaryResponseDTO;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.dto.PageResponse;
import learntime.backend.global.security.CustomPasswordEncoder;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final CustomPasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    @Transactional(readOnly = true)
    public MyPageResponseDTO getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        return UserConverter.toMyPageResponseDTO(user);
    }

    @Transactional
    public AuthService.TokenPair updateName(String email, String name) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        if (userRepository.existsByName(name)) {
            throw new AuthException(AuthErrorCode.USER_NAME_DUPLICATED);
        }
        user.updateInfo(name);
        return authService.generateTokenPair(user);
    }

    @Transactional
    public void updatePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AuthException(AuthErrorCode.PASSWORD_NOT_MATCH);
        }
        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    /** 마이페이지 요약 통계 조회 */
    @Transactional(readOnly = true)
    public MyPageSummaryResponseDTO getMySummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        long postCount = postRepository.countByUserId(user.getUserId());
        long commentCount = commentRepository.countByUserId(user.getUserId());
        long totalLike = postLikeRepository.sumLikeCountByAuthorId(user.getUserId());
        return MyPageSummaryResponseDTO.builder()
                .postCount(postCount)
                .commentCount(commentCount)
                .totalLikeReceived(totalLike)
                .point(user.getPoint())
                .build();
    }

    /** 내가 쓴 게시글 오프셋 페이지 조회 */
    @Transactional(readOnly = true)
    public PageResponse<PostListResponseDTO> getMyPosts(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        Page<Post> posts = postRepository.findMyPosts(user.getUserId(), pageable);

        // 댓글 수 일괄 조회
        java.util.List<Long> postIds = posts.getContent().stream().map(Post::getPostId).toList();
        Map<Long, Long> commentCountMap;
        if (postIds.isEmpty()) {
            commentCountMap = Map.of();
        } else {
            commentCountMap = commentRepository.countCommentsByPostIds(postIds).stream()
                    .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1], (a, b) -> a));
        }

        Page<PostListResponseDTO> dtoPage = posts.map(post ->
                PostConverter.toPostListResponseDTO(post, commentCountMap.getOrDefault(post.getPostId(), 0L)));
        return PageResponse.of(dtoPage);
    }

    /** 내가 쓴 댓글 오프셋 페이지 조회 */
    @Transactional(readOnly = true)
    public PageResponse<MyCommentListResponseDTO> getMyComments(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        Page<Comment> comments = commentRepository.findMyComments(user.getUserId(), pageable);
        return PageResponse.of(comments.map(MyCommentListResponseDTO::from));
    }

}
