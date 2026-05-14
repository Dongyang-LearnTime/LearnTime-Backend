package learntime.backend.domain.community.service.core;

import learntime.backend.domain.community.converter.CommentConverter;
import learntime.backend.domain.community.dto.response.CommentResponseDTO;
import learntime.backend.domain.community.error.code.CommunityErrorCode;
import learntime.backend.domain.community.error.exception.CommunityException;
import learntime.backend.domain.community.model.Comment;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import learntime.backend.domain.community.dto.request.CommentCreateRequestDTO;
import learntime.backend.domain.community.dto.request.CommentUpdateRequestDTO;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /** 특정 게시글에 작성된 댓글 목록을 커서 기반으로 조회 */
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getCommentsByPostId(Long postId, Long lastCommentId, int size) {
        Pageable pageable = PageRequest.of(0, size);
        List<Comment> comments;
        if (lastCommentId == null) { // 첫 페이지
            comments = commentRepository.findFirstPageByPostIdWithUser(postId, pageable);
        } else {
            // 그 외 페이지
            comments = commentRepository.findByPostIdWithUserWithCursor(postId, lastCommentId, pageable);
        }

        return comments.stream()
                .map(CommentConverter::toCommentResponseDTO)
                .toList();
    }

    /** 댓글을 생성함. */
    @Transactional
    public Long createComment(CommentCreateRequestDTO request, Long userId) {
        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        Comment comment = Comment.builder()
                .content(request.content())
                .post(post)
                .user(user)
                .build();

        return commentRepository.save(comment).getCommentId();
    }

    /** 댓글을 수정함 */
    @Transactional
    public void updateComment(Long commentId, CommentUpdateRequestDTO request, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.COMMENT_NOT_FOUND));

        // 본인 확인
        AuthorizationUtil.verifyOwnership(userId, comment.getUser().getUserId());

        comment.updateContent(request.content());
    }

    /** 특정 댓글을 삭제합니다 (Soft Delete). */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.COMMENT_NOT_FOUND));

        // 본인 확인
        AuthorizationUtil.verifyOwnership(userId, comment.getUser().getUserId());

        commentRepository.delete(comment);
    }

}
