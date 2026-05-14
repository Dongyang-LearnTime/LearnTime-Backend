package learntime.backend.domain.community.converter;

import learntime.backend.domain.community.dto.response.CommentResponseDTO;
import learntime.backend.domain.community.model.Comment;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class CommentConverter {

    public CommentConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static CommentResponseDTO toCommentResponseDTO(Comment comment) {
        Long authorId = comment.getUser() != null ? comment.getUser().getUserId() : null;
        String authorName = comment.getUser() != null ? comment.getUser().getName() : "탈퇴한 사용자";

        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .authorId(authorId)
                .authorName(authorName)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

}
