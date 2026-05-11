package learntime.backend.domain.user.dto.response;

import learntime.backend.domain.user.enums.Role;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record MyPageResponseDTO(
        String email,
        String userName,
        Integer point,
        String socialProvider,
        Map<String, Boolean> termsAgreements,
        LocalDateTime createdAt,
        Role role
) {}
