package learntime.backend.domain.studymember.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "공부 스터디 맴버 초대 요청 정보를 담은 DTO")
public record StudyMemberRequestDTO(
        @NotNull(message = "공부 진도 ID는 필수입니다.")
        Long studyId,

        @NotNull(message = "초대 받은 사용자 ID는 필수입니다.")
        Long invitedUserId
) {
}
