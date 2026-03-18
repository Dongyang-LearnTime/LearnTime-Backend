package learntime.backend.domain.study.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

// Gemini 응답 파싱용
@Getter @Setter
public class StudyPlanResponseDTO {
    @JsonProperty("daily_plans")
    private List<DailyPlan> dailyPlans;

    @Getter @Setter
    public static class DailyPlan {
        private int day;
        private List<Task> tasks;
    }

    @Getter @Setter
    public static class Task {
        @JsonProperty("chapter_title")
        private String chapterTitle;

        private String difficulty; // "쉬움", "보통", "어려움"
    }
}
