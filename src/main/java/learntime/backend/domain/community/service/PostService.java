package learntime.backend.domain.community.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.community.converter.PostConverter;
import learntime.backend.domain.community.dto.request.PostCreateRequestDTO;
import learntime.backend.domain.community.dto.request.PostUpdateRequestDTO;
import learntime.backend.domain.community.error.code.CommunityErrorCode;
import learntime.backend.domain.community.error.exception.CommunityException;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.model.PostImage;
import learntime.backend.domain.community.repository.PostImageRepository;
import learntime.backend.domain.community.repository.PostLikeRepository;
import learntime.backend.domain.community.repository.PostRepository;

import learntime.backend.domain.study_progress.dto.response.StudyTotalInfoResponseDTO;
import learntime.backend.domain.study_progress.service.StudyQueryService;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.infra.s3.S3Service;
import learntime.backend.global.utils.AuthorizationUtil;
import learntime.backend.global.utils.FileValidatorUtil;
import learntime.backend.global.infra.s3.event.ImageDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private final StudyQueryService studyQueryService;
    private final S3Service s3Service;
    private final FileValidatorUtil fileValidatorUtil;
    private final PostLikeRepository postLikeRepository;
    private final PostImageRepository postImageRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final int IMAGE_LIMIT_COUNT = 3;

    /** 게시글 수정 */
    @Transactional
    public void updatePost(Long postId, PostUpdateRequestDTO request, List<MultipartFile> newImages, Long userId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));

        Long postAuthorId = post.getUser() != null ? post.getUser().getUserId() : null;
        AuthorizationUtil.verifyOwnership(userId, postAuthorId);

        // 스터디 스냅샷 업데이트 로직
        String studySnapshot = post.getStudySnapshot();
        Long newStudyId = request.studyId();

        if (newStudyId == null) {
            studySnapshot = null;
        } else {
            try {
                StudyTotalInfoResponseDTO studyIndicator = studyQueryService.getStudyMemberTotalIndicatorByUserId(newStudyId, userId);
                studySnapshot = objectMapper.writeValueAsString(studyIndicator);
            } catch (Exception e) {
                log.warn("수정 중 공부 정보 스냅샷 갱신에 실패했습니다. studyId: {}", newStudyId, e);
            }
        }

        // 본문 업데이트
        post.updatePost(request.title(), request.content(), newStudyId, studySnapshot);

        // 이미지 삭제 처리
        if (request.deletedImageUrls() != null && !request.deletedImageUrls().isEmpty()) {
            List<PostImage> imagesToRemove = post.getImages().stream()
                    .filter(image -> request.deletedImageUrls().contains(image.getFileUrl()))
                    .toList();
            
            for (PostImage imageToRemove : imagesToRemove) {
                eventPublisher.publishEvent(new ImageDeletedEvent(imageToRemove.getFileUrl()));
                post.getImages().remove(imageToRemove);
            }
        }

        // 새 이미지 추가
        handleImageUploads(newImages, post);
    }

    /** 게시글 생성 */
    @Transactional
    public Long createPost(PostCreateRequestDTO request, List<MultipartFile> images, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        if (request.isNotice() && user.getRole() != Role.ROLE_ADMIN) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 공부 정보 스냅샷 생성
        String studySnapshot = null;
        if (request.studyId() != null) {
            try {
                StudyTotalInfoResponseDTO studyIndicator = studyQueryService.getStudyMemberTotalIndicatorByUserId(request.studyId(), userId);
                studySnapshot = objectMapper.writeValueAsString(studyIndicator);
            } catch (Exception e) {
                log.warn("공부 정보 스냅샷 생성 중 오류가 발생했습니다. studyId: {}", request.studyId(), e);
            }
        }

        Post post = PostConverter.toPost(request, studySnapshot, user);

        handleImageUploads(images, post);

        Post savedPost = postRepository.save(post);
        return savedPost.getPostId();
    }

    /** 게시글과 연관된 세부 정보를 한 번에 조회함 */
    @Transactional(readOnly = true)
    public Post getPostWithDetails(Long postId) {
        return postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
    }

    /** 특정 사용자가 해당 게시글을 좋아요 했는지 확인함 */
    @Transactional(readOnly = true)
    public boolean isPostLikedByUser(Long postId, Long userId) {
        return postLikeRepository.existsByPost_PostIdAndUser_UserId(postId, userId);
    }

    /** 이미지 URL 목록 조회 */
    @Transactional(readOnly = true)
    public List<String> getPostImageUrls(Long postId) {
        try {
            return postImageRepository.findFileUrlsByPostId(postId);
        } catch (Exception e) {
            log.warn("게시글 이미지 조회 중 오류가 발생했습니다. postId: {}", postId, e);
            return null;
        }
    }

    // 좋아요 수정 로직
    @Transactional
    public Integer togglePostLike(Long postId, Long userId) {
        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        boolean isLiked = postLikeRepository.existsByPost_PostIdAndUser_UserId(postId, userId);

        // 이미 추천을 누른 경우 -> 추천 취소
        if (isLiked) {
            postLikeRepository.deleteByPost_PostIdAndUser_UserId(postId, userId);
            post.decrementLikeCount();
            return post.getLikeCount();
        }

        // 추천을 누르지 않은 경우 -> 추천 추가
        postLikeRepository.save(PostConverter.toPostLike(post, user));
        post.incrementLikeCount();

        return post.getLikeCount();
    }

    /** 특정 게시글을 삭제합니다 (Soft Delete). */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));

        User currentUser = userRepository.findById(userId)
                        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 삭제 권한 확인
        Long postAuthorId = post.getUser() != null ? post.getUser().getUserId() : null;
        AuthorizationUtil.validateOwnerOrAdmin(currentUser, postAuthorId);

        // 게시글 삭제 시 S3 이미지도 즉시 삭제 처리되도록 이벤트 발행
        if (post.getImages() != null) {
            for (PostImage image : post.getImages()) {
                eventPublisher.publishEvent(new ImageDeletedEvent(image.getFileUrl()));
            }
        }

        postRepository.delete(post);
    }


    /** 게시글에 이미지 추가 */
    private void handleImageUploads(List<MultipartFile> images, Post post) {
        if (images != null && !images.isEmpty()) {
            if (post.getImages().size() + images.size() > IMAGE_LIMIT_COUNT) {
                throw new CommunityException(CommunityErrorCode.IMAGE_LIMIT_EXCEEDED);
            }

            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    fileValidatorUtil.validateImage(image);
                    String fileUrl = s3Service.uploadFile(image, "posts");

                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                try {
                                    s3Service.deleteFile(fileUrl);
                                    log.info("DB 트랜잭션 롤백으로 인해 S3 파일 회수 완료: {}", fileUrl);
                                } catch (Exception e) {
                                    log.error("롤백 후 S3 파일 회수 실패: {}", fileUrl, e);
                                }
                            }
                        }
                    });

                    String originalFileName = image.getOriginalFilename();
                    if (originalFileName == null || "blob".equalsIgnoreCase(originalFileName)) {
                        originalFileName = "image_" + System.currentTimeMillis() + FileValidatorUtil.getExtension(image.getContentType());
                    }

                    PostImage postImage = PostImage.builder()
                            .fileUrl(fileUrl)
                            .originalFileName(originalFileName)
                            .build();

                    post.addImage(postImage);
                }
            }
        }
    }


}
