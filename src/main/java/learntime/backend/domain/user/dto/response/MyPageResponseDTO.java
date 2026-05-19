package learntime.backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.user.enums.Role;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@Schema(description = "마이페이지 내 정보 응답 DTO")
public record MyPageResponseDTO(
        String email,                         // 사용자 이메일
        String userName,                      // 사용자 이름
        Integer point,                        // 사용자 보유 포인트
        String socialProvider,                // 소셜 로그인 제공자
        Map<String, Boolean> termsAgreements, // 약관 동의 내역
        LocalDateTime createdAt,              // 가입 일시
        Role role                             // 사용자 권한
) {}
