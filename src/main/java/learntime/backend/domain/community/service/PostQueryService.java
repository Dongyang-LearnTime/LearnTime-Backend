package learntime.backend.domain.community.service;

import learntime.backend.domain.community.converter.PostConverter;
import learntime.backend.domain.community.dto.response.CommentResponseDTO;
import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import learntime.backend.domain.community.dto.response.PostResponseDTO;
import learntime.backend.domain.community.dto.response.PostUpdateDetailDTO;
import learntime.backend.domain.community.enums.PostSearchType;
import learntime.backend.domain.community.error.code.CommunityErrorCode;
import learntime.backend.domain.community.error.exception.CommunityException;
import learntime.backend.domain.community.event.PostViewEventDTO;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.relationship.repository.UserBlockRepository;
import learntime.backend.domain.study.service.core.StudyQueryService;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostQueryService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserBlockRepository userBlockRepository;

    private final StudyQueryService studyQueryService;
    private final PostService postService;
    private final CommentService commentService;

    private final ApplicationEventPublisher eventPublisher;

    /** 게시글의 상세 정보를 가져오기 위해 여러 서비스(Post, Comment)를 조율함 */
    public PostResponseDTO getPostDetails(Long postId, Long userId, String ipAddress, Long lastCommentId, int size) {
        Post post = postService.getPostWithDetails(postId);

        List<String> imageUrls = postService.getPostImageUrls(postId);
        boolean isImageLoadSuccessful = imageUrls != null;
        List<String> safeImageUrls = isImageLoadSuccessful ? imageUrls : List.of();

        List<CommentResponseDTO> comments = commentService.getCommentsByPostId(postId, lastCommentId, size, userId);

        boolean isGuest = userId == null;

        boolean isLiked = !isGuest &&
                postService.isPostLikedByUser(postId, userId);

        Set<Long> blockedIds = isGuest
                ? Collections.emptySet()
                : userBlockRepository.findBlockedUserIds(userId);

        eventPublisher.publishEvent(new PostViewEventDTO(postId, ipAddress));

        return PostConverter.toPostResponseDTO(
                post,
                safeImageUrls,
                isImageLoadSuccessful,
                comments,
                isLiked,
                isGuest ? null : blockedIds.contains(post.getUser().getUserId())
        );
    }

    /** 오프셋 기반 게시글 목록 페이징 조회 */
    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> getPostList(Pageable pageable, Long userId) {
        Page<Post> posts = postRepository.findAllPosts(pageable);
        return convertToPostListResponsePage(posts, userId);
    }

    /** 제목 또는 내용으로 게시글 목록 페이징 검색 */
    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> searchPosts(
            String keyword,
            PostSearchType type,
            Pageable pageable,
            Long userId
    ) {
        Page<Post> posts = switch (type) {
            case AUTHOR ->
                    postRepository.searchByAuthorName(keyword, pageable);
            case CONTENT ->
                    postRepository.searchByKeyword(keyword, pageable);
        };
        return convertToPostListResponsePage(posts, userId);
    }

    /** 주간 인기글 3개 조회 */
    @Transactional(readOnly = true)
    public List<PostListResponseDTO> getWeeklyPopularPosts(Pageable pageable, Long userId) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<Post> posts = postRepository.findWeeklyPopularPosts(oneWeekAgo, pageable);
        return convertToPostListResponseList(posts, userId);
    }

    /** 공지사항 목록 조회 */
    @Transactional(readOnly = true)
    public List<PostListResponseDTO> getNoticePosts(Long userId) {
        List<Post> posts = postRepository.findNoticePosts();
        return convertToPostListResponseList(posts, userId);
    }

    private Map<Long, Long> getCommentCountMap(List<Post> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream()
                .map(Post::getPostId)
                .toList();
        List<Object[]> results = commentRepository.countCommentsByPostIds(postIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a
                ));
    }

    /** 게시글 수정용 상세 정보 조회 */
    @Transactional(readOnly = true)
    public PostUpdateDetailDTO getPostForUpdate(Long postId, Long userId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, post.getUser().getUserId());

        List<String> imageUrls = getSafeImageUrls(postId);

        String studyTitle = null;
        if (post.getStudyId() != null) {
            try {
                studyTitle = studyQueryService.getStudyTitle(post.getStudyId());
            } catch (Exception e) {
                log.warn("게시글 수정 상세 조회 중 스터디 제목 조회에 실패했습니다. studyId: {}", post.getStudyId(), e);
            }
        }

        return PostUpdateDetailDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .images(imageUrls)
                .studyId(post.getStudyId())
                .studyTitle(studyTitle)
                .build();
    }


    private Set<Long> getBlockedUserIds(Long userId) {
        return userId == null
                ? Collections.emptySet()
                : userBlockRepository.findBlockedUserIds(userId);
    }

    private Boolean getHasBlocked(
            Long currentUserId,
            Set<Long> blockedIds,
            Long targetUserId
    ) {
        return currentUserId == null
                ? null
                : blockedIds.contains(targetUserId);
    }

    private Page<PostListResponseDTO> convertToPostListResponsePage(Page<Post> posts, Long userId) {
        Map<Long, Long> commentCountMap = getCommentCountMap(posts.getContent());
        Set<Long> blockedIds = getBlockedUserIds(userId);

        return posts.map(post -> PostConverter.toPostListResponseDTO(
                post,
                commentCountMap.getOrDefault(post.getPostId(), 0L),
                getHasBlocked(userId, blockedIds, post.getUser().getUserId())
        ));
    }

    private List<PostListResponseDTO> convertToPostListResponseList(List<Post> posts, Long userId) {
        Map<Long, Long> commentCountMap = getCommentCountMap(posts);
        Set<Long> blockedIds = getBlockedUserIds(userId);

        return posts.stream()
                .map(post -> PostConverter.toPostListResponseDTO(
                        post,
                        commentCountMap.getOrDefault(post.getPostId(), 0L),
                        getHasBlocked(userId, blockedIds, post.getUser().getUserId())
                ))
                .toList();
    }

    private List<String> getSafeImageUrls(Long postId) {
        List<String> imageUrls = postService.getPostImageUrls(postId);
        return imageUrls != null ? imageUrls : List.of();
    }

}
