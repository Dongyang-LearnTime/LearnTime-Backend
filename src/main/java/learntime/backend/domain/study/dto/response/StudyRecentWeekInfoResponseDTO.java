package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "최근 일주일 일일 학습 상태 응답 DTO")
public record StudyRecentWeekInfoResponseDTO(
        @Schema(description = "학습 계획 날짜")
        LocalDate planDate,

        @Schema(description = "집중 시간 (계획이 없으면 null)", type = "string", pattern = "HH:mm:ss")
        LocalTime focusTime,

        @Schema(description = "진행 상태 (계획이 없으면 null)")
        ProgressStatus progressStatus,

        @Schema(description = "완료 상태 (계획이 없으면 null)")
        CompletionStatus completionStatus,

        @Schema(description = "이해도 점수 (계획이 없으면 null)")
        Integer understandingScore
) {
}
