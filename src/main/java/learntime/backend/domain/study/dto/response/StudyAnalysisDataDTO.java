package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Builder
@Schema(description = "스터디 분석 데이터 응답 DTO")
public record StudyAnalysisDataDTO(
        Double studyCompletionRate,     // 전체 진도 완료율
        Double studySuccessRate,        // 전체 진도 성공률
        Long totalFocusedTime,          // 총 집중 시간 (초 단위)
        List<DailyTopicStats> topicStats // 일별 학습 주제 통계 목록
) {
    @Builder
    @Schema(description = "일별 학습 주제 통계")
    public record DailyTopicStats(
            String topicContent,       // 그 날 공부한 진도 제목 (내용)
            String completionStatus,   // 성공(SUCCESS) 또는 실패(FAILURE) 상태
            Integer understandingScore // 사용자가 입력한 이해도 (1~5점)
    ) {}
}
