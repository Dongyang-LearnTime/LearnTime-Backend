package learntime.backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
@Schema(description = "사용자 획득 배지 정보 응답 DTO")
public record UserBadgeResponseDTO(
        @Schema(description = "배지 타입 (코드)")
        String badgeType,

        @Schema(description = "배지 이름")
        String displayName,

        @Schema(description = "배지 설명")
        String description,

        @Schema(description = "배지 획득 일시")
        LocalDateTime acquiredAt
) {}
