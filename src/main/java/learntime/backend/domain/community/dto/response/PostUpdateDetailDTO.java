package learntime.backend.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Builder
@Schema(description = "게시글 수정용 상세 정보 응답 DTO")
public record PostUpdateDetailDTO(
        @Schema(description = "게시글 식별자(ID)", example = "1")
        Long postId,
        
        @Schema(description = "게시글 제목", example = "수정할 제목입니다.")
        String title,
        
        @Schema(description = "게시글 본문 내용", example = "수정할 내용입니다.")
        String content,
        
        @Schema(description = "기존에 등록된 이미지 URL 목록")
        List<String> images,

        @Schema(description = "연동된 스터디 ID (선택 사항)", example = "1")
        Long studyId,

        @Schema(description = "연동된 스터디 제목 (선택 사항)", example = "자바 공부")
        String studyTitle
) {
}
