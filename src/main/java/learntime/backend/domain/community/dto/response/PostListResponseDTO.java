package learntime.backend.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.community.enums.PostCategory;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "게시글 목록 조회 응답 DTO")
public record PostListResponseDTO(
        @Schema(description = "게시글 식별자(ID)", example = "1")
        Long postId,
        
        @Schema(description = "작성자 식별자(ID). 탈퇴한 경우 null", example = "123")
        Long userId,
        
        @Schema(description = "작성자 이름 또는 닉네임. 탈퇴한 경우 '탈퇴한 사용자'", example = "홍길동")
        String userName,
        
        @Schema(description = "작성자 프로필 이미지 URL", example = "https://s3.ap-northeast-2.amazonaws.com/...")
        String userProfileImageUrl,

        @Schema(description = "현재 로그인한 사용자가 작성자 차단했는지 여부. 로그아웃 상태면 NULL", example = "false")
        Boolean hasBlocked,
        
        @Schema(description = "게시글 제목", example = "오늘 공부 인증합니다!")
        String title,
        
        @Schema(description = "게시글 조회수", example = "150")
        Integer viewCount,
        
        @Schema(description = "게시글 좋아요 수", example = "23")
        Integer likeCount,
        
        @Schema(description = "게시글에 달린 댓글 수", example = "5")
        Long commentCount,
        
        @Schema(description = "게시글 생성 일시")
        LocalDateTime createdAt,

        @Schema(description = "연동된 스터디 ID (없는 경우 null)", example = "1")
        Long studyId,

        @Schema(description = "연동된 스터디 제목", example = "이펙티브 자바 완독 스터디")
        String studyTitle,

        @Schema(description = "현재 스터디 참여 인원 수 (최대 4명)", example = "2")
        Integer currentMemberCount,

        @Schema(description = "스터디 모집 마감 여부 (정원 4명 도달 시 true)", example = "false")
        Boolean isFull,

        @Schema(description = "게시글 카테고리 (FREE, RECRUITMENT)", example = "FREE")
        PostCategory category,

        @Schema(description = "공지사항 여부", example = "false")
        Boolean isNotice
) {
    public PostListResponseDTO {
        if (userName == null) {
            userName = "탈퇴한 사용자";
        }
    }
}
