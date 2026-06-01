package learntime.backend.domain.community.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.community.dto.request.PostCreateRequestDTO;
import learntime.backend.domain.community.dto.response.CommentResponseDTO;
import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import learntime.backend.domain.community.dto.response.PostResponseDTO;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.model.PostLike;
import learntime.backend.domain.study.dto.response.StudyTotalInfoResponseDTO;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class PostConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public PostConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static PostResponseDTO toPostResponseDTO(
            Post post,
            List<String> imageUrls,
            Boolean isImageLoadSuccessful,
            List<CommentResponseDTO> comments,
            Boolean isLiked,
            Boolean hasBlocked
        ) {
        Long userId = post.getUser() != null ? post.getUser().getUserId() : null;

        String userName = post.getUser() != null ? post.getUser().getName() : "탈퇴한 사용자";
        String userProfileImageUrl = (post.getUser() != null && post.getUser().getProfile() != null)
                ? post.getUser().getProfile().getProfileImageUrl()
                : null;

        // JSON 스냅샷에서 공부 정보 추출
        StudyTotalInfoResponseDTO studyIndicator = null;
        if (post.getStudySnapshot() != null && !post.getStudySnapshot().isBlank()) {
            try {
                studyIndicator = objectMapper.readValue(post.getStudySnapshot(), StudyTotalInfoResponseDTO.class);
            } catch (Exception e) {
                // 역직렬화 실패 시 null 유지
            }
        }

        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .userId(userId)
                .userName(userName)
                .userProfileImageUrl(userProfileImageUrl)
                .hasBlocked(hasBlocked)
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .isLiked(isLiked)
                .isImageLoadSuccessful(isImageLoadSuccessful)
                .images(imageUrls)
                .comments(comments)
                .studyTotalIndicator(studyIndicator)
                .studyId(post.getStudyId())
                .isNotice(post.isNotice())
                .build();
    }

    public static Post toPost (PostCreateRequestDTO request, String studySnapshot, User user) {
        return Post.builder()
                .title(request.title())
                .content(request.content())
                .user(user)
                .studyId(request.studyId())
                .studySnapshot(studySnapshot)
                .isNotice(request.isNotice())
                .build();
    }

    public static PostLike toPostLike(Post post, User user) {
        return PostLike.builder()
                .post(post)
                .user(user)
                .build();
    }

    public static PostListResponseDTO toPostListResponseDTO(Post post, Long commentCount, Boolean hasBlocked) {
        Long userId = post.getUser() != null ? post.getUser().getUserId() : null;
        String userName = post.getUser() != null ? post.getUser().getName() : "탈퇴한 사용자";
        String userProfileImageUrl = (post.getUser() != null && post.getUser().getProfile() != null)
                ? post.getUser().getProfile().getProfileImageUrl()
                : null;

        return PostListResponseDTO.builder()
                .postId(post.getPostId())
                .userId(userId)
                .userName(userName)
                .userProfileImageUrl(userProfileImageUrl)
                .hasBlocked(hasBlocked)
                .title(post.getTitle())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .isNotice(post.isNotice())
                .build();
    }




}
