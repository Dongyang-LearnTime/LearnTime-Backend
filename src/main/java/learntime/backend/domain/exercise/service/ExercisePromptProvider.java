package learntime.backend.domain.exercise.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

@Component
@Getter
@Slf4j
public class ExercisePromptProvider {

    @Value("classpath:prompts/weekly-analysis-prompt.txt")
    private Resource weeklyAnalysisPromptResource;

    @Value("classpath:prompts/exercise-calorie-prompt.txt")
    private Resource exerciseCaloriePromptResource;

    @Value("classpath:prompts/meal-analysis-prompt.txt")
    private Resource mealAnalysisPromptResource;

    private String weeklyAnalysisPrompt;
    private String exerciseCaloriePrompt;
    private String mealAnalysisPrompt;

    @PostConstruct
    public void init() {
        try {
            this.weeklyAnalysisPrompt = weeklyAnalysisPromptResource.getContentAsString(StandardCharsets.UTF_8);
            this.exerciseCaloriePrompt = exerciseCaloriePromptResource.getContentAsString(StandardCharsets.UTF_8);
            this.mealAnalysisPrompt = mealAnalysisPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("운동/식단 도메인 프롬프트 템플릿 초기화 실패", e);
            throw new BusinessException(ErrorCode.PROMPT_INIT_FAILED);
        }
    }
}
