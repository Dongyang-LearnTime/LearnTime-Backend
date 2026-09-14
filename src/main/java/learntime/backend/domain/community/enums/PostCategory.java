package learntime.backend.domain.community.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 카테고리 (FREE: 일반/자유/공부인증, RECRUITMENT: 스터디원 모집)")
public enum PostCategory {
    FREE,
    RECRUITMENT
}
