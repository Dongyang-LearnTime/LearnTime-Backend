package learntime.backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "친구 정보 응답 DTO")
public record FriendResponseDTO(
        Long friendId,          // 친구 관계 식별자
        Long userId,            // 친구의 사용자 식별자
        String name,            // 친구 이름
        String email,           // 친구 이메일
        LocalDateTime createdAt // 친구가 된 일시
) {
}
