package learntime.backend.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.community.model.Comment;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "내가 쓴 댓글 조회 응답 DTO")
public record MyCommentListResponseDTO(
        @Schema(description = "댓글 식별자(ID)", example = "10")
        Long commentId,
        
        @Schema(description = "게시글 식별자(ID)", example = "5")
        Long postId,

        @Schema(description = "게시글 제목", example = "오늘 공부 인증합니다!")
        String postTitle,
        
        @Schema(description = "댓글 본문 내용", example = "좋은 글 감사합니다!")
        String content,
        
        @Schema(description = "댓글 생성 일시")
        LocalDateTime createdAt,
        
        @Schema(description = "댓글 수정 일시")
        LocalDateTime updatedAt
) { }
