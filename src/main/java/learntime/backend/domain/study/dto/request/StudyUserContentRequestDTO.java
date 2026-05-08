package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 정의 계획 내용 추가 요청 DTO")
public record StudyUserContentRequestDTO(
        @NotNull(message = "일일 계획 ID는 필수입니다.")
        Long studyDailyPlanId,

        @NotBlank(message = "내용은 비어있을 수 없습니다.")
        @Size(max = 150, message = "사용자 진도 내용은 150자 이하여야 합니다.")
        String userContent
) {}
