package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "스터디 공유 요청 DTO")
public record StudyShareRequestDTO(
        @Schema(description = "공유할 친구의 사용자 ID 목록 (최대 3명)", example = "[1, 2]")
        @Size(max = 3, message = "공유 친구는 최대 3명까지 가능합니다.")
        List<Long> sharedFriendIds
) {
}
