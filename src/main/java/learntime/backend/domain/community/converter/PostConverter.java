package learntime.backend.domain.community.converter;

import learntime.backend.domain.community.dto.response.CommentResponseDTO;
import learntime.backend.domain.community.dto.response.PostResponseDTO;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.study.dto.response.StudyTotalInfoResponseDTO;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

import java.util.List;

public class PostConverter {

    public PostConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static PostResponseDTO toPostResponseDTO(Post post,
                                                    List<String> imageUrls,
                                                    Boolean isImageLoadSuccessful,
                                                    List<CommentResponseDTO> comments,
                                                    StudyTotalInfoResponseDTO studyIndicator,
                                                    Boolean isLiked) {
        Long userId = post.getUser() != null ? post.getUser().getUserId() : null;
        String userName = post.getUser() != null ? post.getUser().getName() : "탈퇴한 사용자";
        Long studyId = post.getStudy() != null ? post.getStudy().getStudyId() : null;

        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .userId(userId)
                .userName(userName)
                .studyId(studyId)
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
                .build();
    }

}
