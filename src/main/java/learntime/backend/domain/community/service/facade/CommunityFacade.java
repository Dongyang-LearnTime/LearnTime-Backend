package learntime.backend.domain.community.service.facade;

import learntime.backend.domain.community.converter.CommentConverter;
import learntime.backend.domain.community.converter.PostConverter;
import learntime.backend.domain.community.dto.response.CommentResponseDTO;
import learntime.backend.domain.community.dto.response.PostResponseDTO;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.service.core.CommentService;
import learntime.backend.domain.community.service.core.PostService;
import learntime.backend.domain.study.dto.response.StudyTotalInfoResponseDTO;
import learntime.backend.domain.study.service.core.StudyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import learntime.backend.domain.community.dto.PostViewEventDTO;

@Component
@RequiredArgsConstructor
public class CommunityFacade {

    private final PostService postService;
    private final CommentService commentService;
    private final StudyQueryService studyQueryService;
    private final ApplicationEventPublisher eventPublisher;

    /** 게시글의 상세 정보를 가져오기 위해 여러 서비스(Post, Comment, Study)를 조율함 */
    public PostResponseDTO getPostDetails(Long postId, Long userId, String ipAddress, Long lastCommentId, int size) {
        Post post = postService.getPostWithDetails(postId);

        List<String> imageUrls = postService.getPostImageUrls(postId);
        boolean isImageLoadSuccessful = true;
        if (imageUrls == null) {
            isImageLoadSuccessful = false;
            imageUrls = List.of(); // 프론트엔드 예외 방지를 위해 빈 리스트로 변환
        }

        List<CommentResponseDTO> comments = commentService.getCommentsByPostId(postId, lastCommentId, size);

        StudyTotalInfoResponseDTO studyIndicator = null;
        if (post.getStudy() != null) {
            studyIndicator = studyQueryService.getStudyTotalIndicator(post.getStudy().getStudyId(), userId);
        }

        boolean isLiked = false;
        if (userId != null) {
            isLiked = postService.isPostLikedByUser(postId, userId);
        }

        eventPublisher.publishEvent(new PostViewEventDTO(postId, ipAddress));

        return PostConverter.toPostResponseDTO(post, imageUrls, isImageLoadSuccessful, comments, studyIndicator, isLiked);
    }

}
