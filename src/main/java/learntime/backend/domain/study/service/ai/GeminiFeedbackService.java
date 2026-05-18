package learntime.backend.domain.study.service.ai;

import jakarta.annotation.PostConstruct;
import learntime.backend.domain.study.dto.response.AiFeedbackResponseDTO;
import learntime.backend.domain.study.dto.response.StudyAnalysisDataDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.infra.gemini.GeminiClient;
import learntime.backend.global.utils.GeminiPromptParser;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiFeedbackService {

    private final GeminiClient geminiClient;
    private final GeminiPromptParser promptParser;
    private final PromptQuotaUtil promptQuotaUtil;

    private static final Map<String, Object> FEEDBACK_INSTRUCTION = Map.of(
            "parts", List.of(Map.of("text", "너는 학습 데이터 분석 및 피드백 전문가야."))
    );

    private static final double FEEDBACK_AI_TEMPERATURE = 0.3;

    @Value("classpath:prompts/study-feedback-prompt.txt")
    private Resource feedbackPromptTemplateResource;

    private String feedbackPromptTemplate;

    /**  피드백 생성을 위한 프롬프트 템플릿을 초기화한다. */
    @PostConstruct
    public void init() {
        try {
            this.feedbackPromptTemplate = feedbackPromptTemplateResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("피드백 프롬프트 초기화 실패", e);
            throw new StudyException(StudyErrorCode.PROMPT_INIT_FAILED);
        }
    }

    /** 사용자의 학습 데이터를 바탕으로 AI 피드백을 생성합니다. */
    public AiFeedbackResponseDTO generateStudyFeedback(StudyAnalysisDataDTO analysisData, String userName, Long userId) {
        String totalFocusedTimeStr = analysisData.totalFocusedTime() != null 
                ? formatSeconds(analysisData.totalFocusedTime()) 
                : "0시간 0분";

        String topicStatsStr = analysisData.topicStats().isEmpty() 
                ? "완료된 진도 데이터 없음" 
                : analysisData.topicStats().stream()
                    .map(stat -> String.format("[%s] / %s / %d점", 
                            stat.topicContent(), 
                            "SUCCESS".equals(stat.completionStatus()) ? "달성 성공" : "실패", 
                            stat.understandingScore() != null ? stat.understandingScore() : 0))
                    .collect(Collectors.joining("\n"));

        promptQuotaUtil.decreasePromptQuota(userId);

        String userPrompt = feedbackPromptTemplate.formatted(
                userName,
                analysisData.studyCompletionRate(),
                analysisData.studySuccessRate(),
                totalFocusedTimeStr,
                topicStatsStr
        );

        Map<String, Object> requestBody = promptParser.createRequestBody(
                userPrompt,
                FEEDBACK_INSTRUCTION,
                FEEDBACK_AI_TEMPERATURE
        );

        try {
            String rawJson = geminiClient.sendRequest(requestBody, GeminiModel.GEMINI_3_1);
            return promptParser.parseFeedbackResponse(rawJson);
        } catch (Exception e) {
            log.error("AI 피드백 생성 실패.", e);
            promptQuotaUtil.restorePromptQuota(userId);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    /** 초 단위의 시간을 시간과 분 단위의 문자열로 변환한다. */
    private String formatSeconds(Long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return String.format("%d시간 %d분", hours, minutes);
    }
}
