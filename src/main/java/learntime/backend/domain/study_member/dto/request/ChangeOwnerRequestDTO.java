package learntime.backend.domain.study_member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "공부 스터디 방장 교체 요청 DTO임.")
public record ChangeOwnerRequestDTO(
        @NotNull(message = "공부 진도 ID는 필수입니다.")
        Long studyId,

        @NotNull(message = "새로운 방장 사용자 ID는 필수입니다. (StudyMember ID)")
        Long newOwnerMemberId

) {
}
