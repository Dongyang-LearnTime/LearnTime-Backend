package learntime.backend.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "게시글 댓글 응답 DTO")
public record CommentResponseDTO(
        @Schema(description = "댓글 식별자(ID)", example = "10")
        Long commentId,
        
        @Schema(description = "댓글 작성자 식별자(ID). 탈퇴한 경우 null", example = "123")
        Long authorId,
        
        @Schema(description = "댓글 작성자 이름. 탈퇴한 경우 '탈퇴한 사용자'", example = "김철수")
        String authorName,
        
        @Schema(description = "댓글 본문 내용", example = "좋은 글 감사합니다!")
        String content,
        
        @Schema(description = "댓글 생성 일시")
        LocalDateTime createdAt,
        
        @Schema(description = "댓글 수정 일시")
        LocalDateTime updatedAt
) {
}
