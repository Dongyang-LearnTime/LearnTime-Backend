package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "공부 진도 관련 주요 지표를 종합적으로 전달하는 DTO")
public record StudyTotalInfoResponseDTO(
        @Schema(description = "진도 완료률", example = "87.5")
        Double studyCompletionRate,

        @Schema(description = "진도 성공률", example = "92.3")
        Double studySuccessRate,

        @Schema(description = "퀴즈 정답률", example = "78.4")
        Double quizCorrectRate,

        @Schema(description = "총 집중 시간 (단위: 초)", example = "5400")
        Long totalFocusedTime
) {
}