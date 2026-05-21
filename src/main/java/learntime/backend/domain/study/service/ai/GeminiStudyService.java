package learntime.backend.domain.study.service.ai;

import jakarta.annotation.PostConstruct;
import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.infra.gemini.GeminiClient;
import learntime.backend.global.utils.GeminiPromptParser;
import learntime.backend.global.utils.PromptQuotaUtil;
import learntime.backend.domain.study.service.util.StudyPlanEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
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
    private final StudyPlanEngine studyPlanEngine;

    private static final Map<String, Object> STUDY_SYSTEM_INSTRUCTION = Map.of(
            "parts", List.of(Map.of("text", "너는 도서의 커리큘럼을 짜는 학습 계획 전문가야."))
    );

    private static final Map<String, Object> LIST_SCHEMA = Map.of(
            "type", "ARRAY",
            "items", Map.of("type", "STRING")
    );

    private static final double STUDY_AI_TEMPERATURE = 0.2;

    @Value("classpath:prompts/study-plan-prompt.txt")
    private Resource promptResource;

    @Value("classpath:prompts/replan-study-prompt.txt")
    private Resource replanPromptResource;

    private String promptTemplate;
    private String replanPromptTemplate;


    /** 학습 계획 및 재계획을 위한 프롬프트 템플릿을 초기화한다. */
    @PostConstruct
    public void init() {
        try {
            this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
            this.replanPromptTemplate = replanPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("프롬프트 초기화 실패", e);
            throw new StudyException(StudyErrorCode.PROMPT_INIT_FAILED);
        }
    }

    /** 제공된 도서 정보와 목차를 바탕으로 AI 학습 계획을 생성한다. */
    @Transactional
    public StudyPlanResponseDTO generateSmartStudyPlan(GeminiStudyRequestDTO request, Long userId) {
        int periodDays = request.getValidatedStudyDays();

        // AI가 분배할 목표 일수 보정: 전체 기간 - 복습 여유 7일
        int targetDays = Math.max(1, periodDays - 7);

        String bookToc = IntStream.range(0, request.tocList().size())
                .mapToObj(i -> {
                    String tocString = getTocString(request, i);
                    return tocString.isEmpty() ? "" : (i + "::" + tocString);
                })
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));

        String userPrompt = promptTemplate.formatted(
                targetDays,
                bookToc
        );

        // AI로부터 인덱스 조합 결과를 받음 (예: ["0, 1", "2"])
        List<String> indexGroups = executeGeminiListRequest(userPrompt, userId);

        // 인덱스를 다시 원본 텍스트로 치환하여 조립 (빈 문자열 항목은 필터링)
        List<String> distributedTopics = indexGroups.stream()
                .map(group -> Arrays.stream(group.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> {
                            try { return Integer.parseInt(s); }
                            catch (NumberFormatException e) { return -1; }
                        })
                        .filter(idx -> idx >= 0)
                        .map(idx -> getTocString(request, idx))
                        .filter(text -> !text.isBlank())
                        .collect(Collectors.joining(", ")))
                .filter(topic -> !topic.isBlank())
                .collect(Collectors.toList());

        return studyPlanEngine.buildFullPlan(distributedTopics, periodDays);
    }

    /** 인덱스에 해당하는 목차의 텍스트를 생성하여 반환한다. */
    private String getTocString(GeminiStudyRequestDTO request, int index) {
        if (index < 0 || index >= request.tocList().size()) return "";

        TocListResponseDTO current = request.tocList().get(index);
        String chapter = Objects.toString(current.chapter(), "");
        String title = Objects.toString(current.title(), "");
        String weightSuffix = buildWeightSuffix(request, index);

        return (chapter + " " + title + weightSuffix).trim();
    }

    /** 남은 학습 내용과 기간을 바탕으로 AI 재계획을 생성한다. */
    @Transactional
    public StudyPlanResponseDTO generateReplan(GeminiReplanRequestDTO request, String remainingContent, int remainingDays, Long userId) {
        long contentCount = Arrays.stream(remainingContent.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .count();
        if (contentCount == 0) {
            contentCount = 1;
        }

        int targetDays = Math.max(1, remainingDays - 7);

        String userPrompt = replanPromptTemplate.formatted(
                targetDays,
                request.studyTitle(),
                remainingContent
        );

        List<String> distributedTopics = executeGeminiListRequest(userPrompt, userId);
        return studyPlanEngine.buildFullPlan(distributedTopics, remainingDays);
    }

    /** Gemini 모델에 요청을 보내고 단순 목차 리스트를 반환한다. */
    private List<String> executeGeminiListRequest(String userPrompt, Long userId) {
        promptQuotaUtil.decreasePromptQuota(userId);

        Map<String, Object> requestBody = promptParser.createRequestBody(
                userPrompt,
                STUDY_SYSTEM_INSTRUCTION,
                STUDY_AI_TEMPERATURE,
                LIST_SCHEMA
        );

        try {
            String rawJson = geminiClient.sendRequest(requestBody, GeminiModel.GEMINI_3_1);
            return promptParser.parseListResponse(rawJson);

        } catch (Exception e) {
            log.error("AI 학습 계획 생성 실패.", e);
            promptQuotaUtil.restorePromptQuota(userId);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    /** 목차 항목 간의 페이지 차이를 계산하여 가중치 접미사를 생성한다. */
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
