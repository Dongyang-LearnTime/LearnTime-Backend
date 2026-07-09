package learntime.backend.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Role;
import java.time.LocalDateTime;

@Schema(description = "관리자용 사용자 상세 응답 DTO")
public record AdminUserDetailResponseDTO(
        @Schema(description = "사용자 ID") Long userId,
        @Schema(description = "이메일") String email,
        @Schema(description = "이름(닉네임)") String name,
        @Schema(description = "보유 포인트") Integer point,
        @Schema(description = "가입 경로") AuthProvider socialProvider,
        @Schema(description = "권한") Role role,
        @Schema(description = "가입일") LocalDateTime createdAt,
        @Schema(description = "최종 수정일") LocalDateTime updatedAt,
        @Schema(description = "로그인 실패 횟수") Integer failedAttempts,
        @Schema(description = "계정 잠금 상태") Boolean isLocked,
        @Schema(description = "계정 잠금 발생 시간") LocalDateTime lockedAt,
        @Schema(description = "AI 할당량 남은 갯수") Integer aiRemainingCount
) {}
