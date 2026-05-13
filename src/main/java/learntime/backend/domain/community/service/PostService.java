package learntime.backend.domain.community.service;

import learntime.backend.domain.community.dto.request.PostCreateRequestDTO;
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
import learntime.backend.global.utils.FileValidatorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final S3Service s3Service;
    private final FileValidatorUtil fileValidatorUtil;

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

        if (images != null && !images.isEmpty()) {
            if (images.size() > 3) {
                throw new IllegalArgumentException("이미지는 최대 3개까지 첨부 가능합니다.");
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

        Post savedPost = postRepository.save(post);
        return savedPost.getPostId();
    }
}
