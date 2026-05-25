package learntime.backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.user.enums.FriendRequestStatus;

import java.time.LocalDateTime;

@Schema(description = "친구 요청 응답 DTO")
public record FriendRequestResponseDTO(
        Long friendRequestId,       // 친구 요청 식별자
        Long requesterId,           // 요청자 식별자
        String requesterName,       // 요청자 이름
        Long receiverId,            // 수신자 식별자
        String receiverName,        // 수신자 이름
        FriendRequestStatus status, // 친구 요청 상태 (대기, 수락, 거절)
        LocalDateTime createdAt     // 친구 요청 일시
) {
}
