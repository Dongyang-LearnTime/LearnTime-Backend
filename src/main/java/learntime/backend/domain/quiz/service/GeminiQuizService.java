package learntime.backend.domain.quiz.service;

import jakarta.annotation.PostConstruct;
import learntime.backend.domain.quiz.dto.response.QuizQuestionResponseDTO;
import learntime.backend.global.utils.GeminiPromptParser;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.infra.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiQuizService {

    private final GeminiClient geminiClient;
    private final GeminiPromptParser promptParser;

    private static final Map<String, Object> STUDY_QUIZ_INSTRUCTION = Map.of(
            "parts", List.of(Map.of("text", "너는 제공된 핵심 노트 텍스트를 바탕으로 학습용 퀴즈를 생성하는 AI 튜터야."))
    );

    private static final double QUIZ_AI_TEMPERATURE = 0.2;

    @Value("classpath:prompts/study-quiz-prompt.txt")
    private Resource quizPromptTemplateResource;

    private String quizPromptTemplate;

    @PostConstruct
    public void init() {
        try {
            this.quizPromptTemplate = quizPromptTemplateResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("퀴즈 프롬프트 초기화 실패", e);
        }
    }

    public List<QuizQuestionResponseDTO> generateQuizQuestions(int totalCount, int oxCount, int multipleCount, String cleanedText) {
        // 프롬프트 생성
        String userPrompt = quizPromptTemplate.formatted(
                totalCount,
                oxCount,
                multipleCount,
                cleanedText
        );

        Map<String, Object> requestBody = promptParser.createRequestBody(
                userPrompt,
                STUDY_QUIZ_INSTRUCTION,
                QUIZ_AI_TEMPERATURE
        );

        try {
            String rawJson = geminiClient.sendRequest(requestBody, GeminiModel.GEMINI_3_0);
            return promptParser.parseQuizResponse(rawJson); // 결과 DTO로 파싱
        } catch (Exception e) {
            log.error("AI 퀴즈 생성 실패.", e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }
}
