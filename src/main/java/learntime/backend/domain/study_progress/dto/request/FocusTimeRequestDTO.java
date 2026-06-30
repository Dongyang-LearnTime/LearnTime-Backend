package learntime.backend.domain.study_progress.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = "공부 일일 집중 시간 등록 요청 DTO")
public record FocusTimeRequestDTO(

        @Schema(description = "일일 공부 진도 ID", example = "1")
        @NotNull(message = "공부 일일 진도 ID는 필수입니다.")
        Long studyDailyPlanId,

        @Schema(description = "집중 시간 (HH:mm:ss)", example = "01:30:00", type = "string")
        @NotNull(message = "집중 시간은 필수입니다.")
        LocalTime focusTime

) { }
