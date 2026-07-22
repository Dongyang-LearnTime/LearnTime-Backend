package learntime.backend.domain.user.service;

import learntime.backend.domain.community.converter.CommentConverter;
import learntime.backend.domain.community.converter.PostConverter;
import learntime.backend.domain.community.dto.response.MyCommentListResponseDTO;
import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import learntime.backend.domain.community.model.Comment;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostLikeRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.relationship.converter.UserBlockConverter;
import learntime.backend.domain.relationship.dto.response.MyBlockedUserListResponseDTO;
import learntime.backend.domain.relationship.model.UserBlock;
import learntime.backend.domain.relationship.repository.UserBlockRepository;
import learntime.backend.domain.user.dto.request.UnlinkGoogleRequestDTO;
import learntime.backend.domain.user.enums.AuthProvider;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final CustomPasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final OAuth2Service oAuth2Service;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserBlockRepository userBlockRepository;


    @Transactional(readOnly = true)
    public MyPageResponseDTO getMyInfo(Long userId) {
        User user = findByUserOrThrow(userId);

        return UserConverter.toMyPageResponseDTO(user);
    }

    @Transactional
    public AuthService.TokenPair updateName(Long userId, String name) {
        User user = findByUserOrThrow(userId);

        if (userRepository.existsByName(name)) {
            throw new AuthException(AuthErrorCode.USER_NAME_DUPLICATED);
        }
        user.updateInfo(name);
        return authService.generateTokenPair(user);
    }

    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = findByUserOrThrow(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AuthException(AuthErrorCode.PASSWORD_NOT_MATCH);
        }
        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void unlinkGoogleAccount(Long userId, UnlinkGoogleRequestDTO request) {
        User user = findByUserOrThrow(userId);

        if (user.getSocialProvider() != AuthProvider.GOOGLE) {
            throw new AuthException(AuthErrorCode.NOT_GOOGLE_USER);
        }

        oAuth2Service.revokeSocialToken(AuthProvider.GOOGLE, request.googleToken());

        user.convertToLocalUser(passwordEncoder.encode(request.newPassword()));
    }


    /** 마이페이지 요약 통계 조회 */
    @Transactional(readOnly = true)
    public MyPageSummaryResponseDTO getMySummary(Long userId) {
        User user = findByUserOrThrow(userId);

        long postCount = postRepository.countByUserId(userId);
        long commentCount = commentRepository.countByUserId(userId);
        long totalLike = postLikeRepository.sumLikeCountByAuthorId(userId);
        return MyPageSummaryResponseDTO.builder()
                .postCount(postCount)
                .commentCount(commentCount)
                .totalLikeReceived(totalLike)
                .point(user.getPoint())
                .build();
    }

    /** 내가 쓴 게시글 오프셋 페이지 조회 */
    @Transactional(readOnly = true)
    public PageResponse<PostListResponseDTO> getMyPosts(Long userId, Pageable pageable) {

        Page<Post> posts = postRepository.findMyPosts(userId, pageable);

        // 댓글 수 일괄 조회
        List<Long> postIds = posts.getContent().stream().map(Post::getPostId).toList();
        Map<Long, Long> commentCountMap;
        if (postIds.isEmpty()) {
            commentCountMap = Map.of();
        } else {
            commentCountMap = commentRepository.countCommentsByPostIds(postIds).stream()
                    .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1], (a, b) -> a));
        }

        Page<PostListResponseDTO> dtoPage = posts.map(post ->
                PostConverter.toPostListResponseDTO(post, commentCountMap.getOrDefault(post.getPostId(), 0L), false));
        return PageResponse.of(dtoPage);
    }

    /** 내가 쓴 댓글 오프셋 페이지 조회 */
    @Transactional(readOnly = true)
    public PageResponse<MyCommentListResponseDTO> getMyComments(Long userId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findMyComments(userId, pageable);

        Page<MyCommentListResponseDTO> response = comments.map(
                CommentConverter::toMyCommentListResponseDTO
        );

        return PageResponse.of(response);
    }

    public PageResponse<MyBlockedUserListResponseDTO> getMyBlockedUsers(Long userId, Pageable pageable) {
        Page <UserBlock> blocks = userBlockRepository.findBlockedUsers(userId, pageable);

        Page <MyBlockedUserListResponseDTO> response = blocks.map(
                UserBlockConverter::toMyBlockedUserListResponseDTO
        );

        return PageResponse.of(response);
    }

    private User findByUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
    }

}
