package learntime.backend.domain.study.service.ai;

import jakarta.annotation.PostConstruct;
import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.infra.gemini.GeminiClient;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Gemini 모델을 이용한 학습 계획 생성 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiStudyService {

    private final GeminiClient geminiClient;
    private final GeminiPromptParser promptParser;
    private final PromptQuotaUtil promptQuotaUtil;

    @Value("classpath:prompts/study-plan-prompt.txt")
    private Resource promptResource;

    @Value("classpath:prompts/replan-study-prompt.txt")
    private Resource replanPromptResource;

    private String promptTemplate;
    private String replanPromptTemplate;

    @PostConstruct
    public void init() {
        try {
            this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
            this.replanPromptTemplate = replanPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("프롬프트 초기화 실패", e);
        }
    }

    @Transactional
    public StudyPlanResponseDTO generateSmartStudyPlan(GeminiStudyRequestDTO request, Long userId) {
        int periodDays = request.getValidatedStudyDays();

        String bookToc = IntStream.range(0, request.tocList().size())
                .mapToObj(i -> {
                    TocListResponseDTO current = request.tocList().get(i);
                    String chapter = Objects.toString(current.chapter(), "");
                    String title = Objects.toString(current.title(), "");
                    String weightSuffix = buildWeightSuffix(request, i);

                    return (chapter + " " + title + weightSuffix).trim();
                })
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));

        String userPrompt = promptTemplate.formatted(
                periodDays,
                request.bookTitle(),
                bookToc
        );

        return executeGeminiRequest(userPrompt, request, userId);
    }

    @Transactional
    public StudyPlanResponseDTO generateReplan(GeminiReplanRequestDTO request, String remainingContent, int remainingDays, Long userId) {
        String userPrompt = replanPromptTemplate.formatted(
                remainingDays,
                request.studyTitle(),
                remainingContent
        );

        return executeGeminiRequest(userPrompt, request, userId);
    }

    private StudyPlanResponseDTO executeGeminiRequest(String userPrompt, Object requestDto, Long userId) {
        promptQuotaUtil.decreasePromptQuota(userId);

        Map<String, Object> requestBody = promptParser.createRequestBody(userPrompt);

        try {
            String rawJson = geminiClient.sendRequest(requestBody, GeminiModel.GEMINI_3_1);
            return promptParser.parseResponse(rawJson);

        } catch (Exception e) {
            log.error("AI 학습 계획 생성 실패. Request: {}", requestDto, e);
            promptQuotaUtil.restorePromptQuota(userId);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private String buildWeightSuffix(GeminiStudyRequestDTO request, int index) {
        TocListResponseDTO current = request.tocList().get(index);
        Integer currentPage = current.page();

        if (currentPage == null || index >= request.tocList().size() - 1) return "";

        Integer nextPage = request.tocList().get(index + 1).page();
        if (nextPage == null || nextPage <= currentPage) return "";

        int diff = nextPage - currentPage;
        return String.format(" (%dp)", diff);
    }

}
