package learntime.backend.domain.study_progress.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study_plan.dto.response.StudyDailyPlanInfoResponseDTO;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "학습 스튜디오 첫 화면 데이터 통합 응답 DTO")
public record StudyStudioSummaryResponseDTO(
        @Schema(description = "조회 기준일의 일일 학습 계획 및 진행 상태")
        StudyDailyPlanInfoResponseDTO todayPlan,

        @Schema(description = "조회 기준일의 사용자 작성 진도 내용")
        StudyMemberContentResponseDTO todayContent,

        @Schema(description = "사용자의 핵심 공부 지표")
        StudyTotalInfoResponseDTO totalIndicator,

        @Schema(description = "스터디 멤버들의 최근 일주일 공부 상태")
        List<StudyMemberRecentWeekInfoResponseDTO> recentWeekIndicator
) {
}
