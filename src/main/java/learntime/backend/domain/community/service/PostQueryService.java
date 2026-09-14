package learntime.backend.domain.community.service;

import learntime.backend.domain.community.converter.PostConverter;
import learntime.backend.domain.community.dto.response.CommentResponseDTO;
import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import learntime.backend.domain.community.dto.response.PostResponseDTO;
import learntime.backend.domain.community.dto.response.PostUpdateDetailDTO;
import learntime.backend.domain.community.enums.PostCategory;
import learntime.backend.domain.community.enums.PostSearchType;
import learntime.backend.domain.community.error.code.CommunityErrorCode;
import learntime.backend.domain.community.error.exception.CommunityException;
import learntime.backend.domain.community.event.PostViewEventDTO;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.relationship.repository.UserBlockRepository;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study_progress.service.StudyQueryService;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostQueryService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserBlockRepository userBlockRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;

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

        String studyTitle = null;
        Integer currentMemberCount = null;
        Boolean isFull = null;

        if (post.getStudyId() != null) {
            Optional<Study> studyOpt = studyRepository.findById(post.getStudyId());
            if (studyOpt.isPresent()) {
                Study study = studyOpt.get();
                studyTitle = study.getStudyTitle();
                long count = studyMemberRepository.countByStudyAndStatusIn(
                        study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)
                );
                currentMemberCount = (int) count;
                isFull = count >= 4;
            }
        }

        return PostConverter.toPostResponseDTO(
                post,
                safeImageUrls,
                isImageLoadSuccessful,
                comments,
                isLiked,
                isGuest || post.getUser() == null ? null : blockedIds.contains(post.getUser().getUserId()),
                studyTitle,
                currentMemberCount,
                isFull
        );
    }

    /** 카테고리별 / 전체 오프셋 기반 게시글 목록 페이징 조회 */
    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> getPostList(Pageable pageable, PostCategory category, Long userId) {
        Page<Post> posts = category != null
                ? postRepository.findAllByCategory(category, pageable)
                : postRepository.findAllPosts(pageable);
        return convertToPostListResponsePage(posts, userId);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> getPostList(Pageable pageable, Long userId) {
        return getPostList(pageable, null, userId);
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

        Long postAuthorId = post.getUser() != null ? post.getUser().getUserId() : null;
        AuthorizationUtil.verifyOwnership(userId, postAuthorId);

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
        Map<Long, StudyMetadata> studyMetadataMap = getStudyMetadataMap(posts.getContent());

        return posts.map(post -> {
            StudyMetadata metadata = post.getStudyId() != null ? studyMetadataMap.get(post.getStudyId()) : null;
            return PostConverter.toPostListResponseDTO(
                    post,
                    commentCountMap.getOrDefault(post.getPostId(), 0L),
                    getHasBlocked(userId, blockedIds, post.getUser() != null ? post.getUser().getUserId() : null),
                    metadata != null ? metadata.title : null,
                    metadata != null ? metadata.memberCount : null,
                    metadata != null ? metadata.isFull : null
            );
        });
    }

    private List<PostListResponseDTO> convertToPostListResponseList(List<Post> posts, Long userId) {
        Map<Long, Long> commentCountMap = getCommentCountMap(posts);
        Set<Long> blockedIds = getBlockedUserIds(userId);
        Map<Long, StudyMetadata> studyMetadataMap = getStudyMetadataMap(posts);

        return posts.stream()
                .map(post -> {
                    StudyMetadata metadata = post.getStudyId() != null ? studyMetadataMap.get(post.getStudyId()) : null;
                    return PostConverter.toPostListResponseDTO(
                            post,
                            commentCountMap.getOrDefault(post.getPostId(), 0L),
                            getHasBlocked(userId, blockedIds, post.getUser() != null ? post.getUser().getUserId() : null),
                            metadata != null ? metadata.title : null,
                            metadata != null ? metadata.memberCount : null,
                            metadata != null ? metadata.isFull : null
                    );
                })
                .toList();
    }

    private Map<Long, StudyMetadata> getStudyMetadataMap(List<Post> posts) {
        List<Long> studyIds = posts.stream()
                .map(Post::getStudyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (studyIds.isEmpty()) {
            return Map.of();
        }

        List<Study> studies = studyRepository.findAllById(studyIds);
        Map<Long, String> studyTitleMap = studies.stream()
                .collect(Collectors.toMap(Study::getStudyId, Study::getStudyTitle, (a, b) -> a));

        List<Object[]> memberCounts = studyMemberRepository.countMembersByStudyIdsAndStatusIn(
                studyIds, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)
        );
        Map<Long, Integer> countMap = memberCounts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue(),
                        (a, b) -> a
                ));

        Map<Long, StudyMetadata> result = new HashMap<>();
        for (Long studyId : studyIds) {
            String title = studyTitleMap.get(studyId);
            int count = countMap.getOrDefault(studyId, 0);
            result.put(studyId, new StudyMetadata(title, count, count >= 4));
        }

        return result;
    }

    private record StudyMetadata(String title, int memberCount, boolean isFull) {}

    private List<String> getSafeImageUrls(Long postId) {
        List<String> imageUrls = postService.getPostImageUrls(postId);
        return imageUrls != null ? imageUrls : List.of();
    }
}
