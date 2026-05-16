package learntime.backend.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study.dto.response.StudyTotalInfoResponseDTO;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "게시글 상세 조회 응답 DTO")
public record PostResponseDTO(
        @Schema(description = "게시글 식별자(ID)", example = "1")
        Long postId,
        
        @Schema(description = "작성자 식별자(ID). 탈퇴한 경우 null", example = "123")
        Long userId,
        
        @Schema(description = "작성자 이름 또는 닉네임. 탈퇴한 경우 '탈퇴한 사용자'", example = "홍길동")
        String userName,
        
        @Schema(description = "게시글 제목", example = "오늘 공부 인증합니다!")
        String title,
        
        @Schema(description = "게시글 본문 내용", example = "열심히 했습니다.")
        String content,
        
        @Schema(description = "게시글 생성 일시")
        LocalDateTime createdAt,
        
        @Schema(description = "게시글 수정 일시")
        LocalDateTime updatedAt,
        
        @Schema(description = "게시글 조회수", example = "150")
        Integer viewCount,
        
        @Schema(description = "게시글 좋아요 수", example = "23")
        Integer likeCount,
        
        @Schema(description = "조회한 사용자의 해당 게시글 좋아요 여부. 비로그인 시 false", example = "true")
        Boolean isLiked,

        @Schema(description = "게시글 이미지 목록 정상 로드 여부. 이미지 로드 중 에러 발생 시 false", example = "true")
        Boolean isImageLoadSuccessful,
        
        @Schema(description = "게시글에 포함된 이미지 URL 목록")
        List<String> images,
        
        @Schema(description = "게시글에 달린 댓글 목록")
        List<CommentResponseDTO> comments,
        
        @Schema(description = "연관된 스터디의 핵심 지표(달성률, 정답률 등). 스냅샷 정보가 없으면 null")
        StudyTotalInfoResponseDTO studyTotalIndicator
) {
}
