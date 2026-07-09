package learntime.backend.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Role;
import java.time.LocalDateTime;

@Schema(description = "관리자용 사용자 리스트 응답 DTO")
public record AdminUserListResponseDTO(
        @Schema(description = "사용자 ID") Long userId,
        @Schema(description = "이메일") String email,
        @Schema(description = "이름(닉네임)") String name,
        @Schema(description = "가입 경로") AuthProvider socialProvider,
        @Schema(description = "권한") Role role,
        @Schema(description = "가입일") LocalDateTime createdAt
) {}
