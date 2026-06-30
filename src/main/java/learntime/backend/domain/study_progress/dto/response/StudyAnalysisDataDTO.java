package learntime.backend.domain.study_progress.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record StudyAnalysisDataDTO(
        Double studyCompletionRate,
        Double studySuccessRate,
        Long totalFocusedTime,
        List<DailyTopicStats> topicStats
) {
    @Builder
    public record DailyTopicStats(
            String topicContent, // 그 날 공부한 진도 제목 (내용)
            String completionStatus, // 성공(SUCCESS) 또는 실패(FAILURE)
            Integer understandingScore // 사용자가 입력한 이해도 (1~5점 등)
    ) {}
}
