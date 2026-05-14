package learntime.backend.domain.community.service.core;

import learntime.backend.domain.community.dto.request.PostCreateRequestDTO;
import learntime.backend.domain.community.error.code.CommunityErrorCode;
import learntime.backend.domain.community.error.exception.CommunityException;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.model.PostImage;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.infra.s3.S3Service;
import learntime.backend.global.utils.AuthorizationUtil;
import learntime.backend.global.utils.FileValidatorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import learntime.backend.domain.community.repository.PostImageRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import learntime.backend.domain.community.dto.response.PostListResponseDTO;

import learntime.backend.domain.community.dto.request.PostUpdateRequestDTO;
import learntime.backend.domain.community.dto.response.PostUpdateDetailDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final S3Service s3Service;
    private final FileValidatorUtil fileValidatorUtil;
    private final learntime.backend.domain.community.repository.PostLikeRepository postLikeRepository;
    private final PostImageRepository postImageRepository;

    private static final int IMAGE_LIMIT_COUNT = 3;

    /** 오프셋 기반 게시글 목록 페이징 조회 */
    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> getPostList(Pageable pageable) {
        return postRepository.findAllPostsWithCommentCount(pageable);
    }

    /** 게시글 수정용 상세 정보 조회 */
    @Transactional(readOnly = true)
    public PostUpdateDetailDTO getPostForUpdate(Long postId, Long userId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, post.getUser().getUserId());

        List<String> imageUrls = getPostImageUrls(postId);
        if (imageUrls == null) {
            imageUrls = List.of();
        }
        
        Long studyId = post.getStudy() != null ? post.getStudy().getStudyId() : null;

        return PostUpdateDetailDTO.builder()
                .postId(post.getPostId())
                .studyId(studyId)
                .title(post.getTitle())
                .content(post.getContent())
                .images(imageUrls)
                .build();
    }

    /** 게시글 수정 */
    @Transactional
    public void updatePost(Long postId, PostUpdateRequestDTO request, List<MultipartFile> newImages, Long userId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, post.getUser().getUserId());

        // 본문 업데이트
        post.updatePost(request.title(), request.content());

        // 이미지 삭제 처리
        if (request.deletedImageUrls() != null && !request.deletedImageUrls().isEmpty()) {
            List<PostImage> imagesToRemove = post.getImages().stream()
                    .filter(image -> request.deletedImageUrls().contains(image.getFileUrl()))
                    .toList();
            
            for (PostImage imageToRemove : imagesToRemove) {
                post.getImages().remove(imageToRemove);
                // s3Service.deleteFile(imageToRemove.getFileUrl()); // S3 실제 삭제는 필요에 따라 추가 또는 배치로 처리
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

        Study study = null;
        if (request.studyId() != null) {
            study = studyRepository.findById(request.studyId())
                    .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
        }

        Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .user(user)
                .study(study)
                .build();

        handleImageUploads(images, post);

        Post savedPost = postRepository.save(post);
        return savedPost.getPostId();
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

    /**  게시글과 연관된 세부 정보(User, Study를 한 번에 조회함. */
    @Transactional(readOnly = true)
    public Post getPostWithDetails(Long postId) {
        return postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
    }

    /** 특정 사용자가 해당 게시글을 좋아요 했는지 확인함. (true시 했음, false시 안함) */
    @Transactional(readOnly = true)
    public boolean isPostLikedByUser(Long postId, Long userId) {
        return postLikeRepository.existsByPost_PostIdAndUser_UserId(postId, userId);
    }

    /** 이미지 조회 실패가 게시글 조회 전체에 영향을 주지 않도록 조회합니다. */
    @Transactional(readOnly = true)
    public List<String> getPostImageUrls(Long postId) {
        try {
            // 엔티티 전체 대신 String(URL)만 조회하여 메모리 및 성능 최적화
            return postImageRepository.findFileUrlsByPostId(postId);
        } catch (Exception e) {
            log.warn("게시글 이미지 조회 중 오류가 발생했습니다. postId: {}", postId, e);
            return null; // 실패 시 null 반환하여 상위에서 인지할 수 있도록 함
        }
    }

    /** 특정 게시글을 삭제합니다 (Soft Delete). */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, post.getUser().getUserId());

        postRepository.delete(post);
    }

}
