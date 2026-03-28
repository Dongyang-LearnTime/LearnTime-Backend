package learntime.backend.domain.exercise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponseDTO {
    private List<AnalysisItem> analysis;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisItem {
        private String title; // 예: 오늘 운동량은 충분합니다.
        private String content; // 예: 소모 칼로리 600kcal로 목표를 달성했습니다.
        private String type; // 예: 칭찬, 개선, 주의 (상태 타입)
    }
}
