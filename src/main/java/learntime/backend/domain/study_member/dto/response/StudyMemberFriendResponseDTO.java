package learntime.backend.domain.study_member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "스터디 초대용 친구 정보 응답 DTO")
public record StudyMemberFriendResponseDTO(
        @Schema(description = "친구 관계 식별자")
        Long friendId,
        @Schema(description = "친구의 사용자 식별자")
        Long userId,
        @Schema(description = "친구 이름")
        String name,
        @Schema(description = "친구 이메일")
        String email,
        @Schema(description = "친구 관계 맺은 일시")
        LocalDateTime createdAt,
        @Schema(description = "스터디 초대(PENDING) 여부")
        Boolean isInvited
) {
}
