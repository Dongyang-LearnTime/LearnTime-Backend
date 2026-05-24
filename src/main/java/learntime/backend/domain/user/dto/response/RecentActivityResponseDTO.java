package learntime.backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.user.enums.RecentActivityType;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
@Schema(description = "최근 학습 활동 정보 DTO")
public record RecentActivityResponseDTO(
        @Schema(description = "활동 유형")
        RecentActivityType type,

        @Schema(description = "활동 상세 ID (필기 ID, 퀴즈 이력 ID 혹은 피드백 ID)")
        Long id,

        @Schema(description = "활동 제목 (필기 제목, 퀴즈 제목 혹은 피드백 제목)")
        String title,

        @Schema(description = "연관된 스터디 제목")
        String studyTitle,

        @Schema(description = "활동 기록/생성/제출 일시")
        LocalDateTime createdAt
) {}
