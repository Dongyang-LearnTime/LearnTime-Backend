package learntime.backend.domain.study.service;

import jakarta.annotation.PostConstruct;
import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.domain.study.service.gemini.GeminiPromptParser;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiStudyService {

    private final GeminiClient geminiClient;
    private final GeminiPromptParser promptParser;
    private final PromptQuotaUtil promptQuotaUtil;

    @Value("classpath:prompts/study-plan-prompt.txt")
    private Resource promptResource; // 진도 프롬프트

    @Value("classpath:prompts/replan-study-prompt.txt")
    private Resource replanPromptResource; // 진도 재설계 프롬프트

    private String promptTemplate;
    private String replanPromptTemplate;

    @PostConstruct
    public void init() {
        try {
            // 파일 → 메모리 캐싱
            this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
            this.replanPromptTemplate = replanPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("프롬프트 초기화 실패", e);
        }
    }

    @Transactional
    public StudyPlanResponseDTO generateSmartStudyPlan(GeminiStudyRequestDTO request, Long userId) {
        int periodDays = request.getValidatedStudyDays(); // 쉬는 날, 요일을 제외한 일수 계산

        // 목차 정보 프롬프트에 들어가게 문장으로 변환
        String bookToc = IntStream.range(0, request.tocList().size())
                .mapToObj(i -> {
                    TocListResponseDTO current = request.tocList().get(i);

                    // 챕터 / 제목이 없을 수도 있으므로 빈 문자열로 처리
                    String chapter = Objects.toString(current.chapter(), "");
                    String title = Objects.toString(current.title(), "");

                    String weightSuffix = buildWeightSuffix(request, i); // 다음 목차와 페이지 차이를 계산해서 분량 정보 추가

                    return (chapter + " " + title + weightSuffix).trim(); // "챕터 제목 (페이지차)" 형태로 한 줄 생성
                })
                .filter(line -> !line.isEmpty()) // 챕터/제목 모두 없는 빈 줄 제거
                .collect(Collectors.joining("\n")); // 줄바꿈으로 이어붙임

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
        // Gemini 이용량 차감
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

    // 페이지 차이(분량) 계산
    private String buildWeightSuffix(GeminiStudyRequestDTO request, int index) {
        TocListResponseDTO current = request.tocList().get(index);

        Integer currentPage = current.page();

        // 현재 목차에 페이지 정보가 없거나 마지막이면 공백
        if (currentPage == null) return "";
        if (index >= request.tocList().size() - 1) return "";

        Integer nextPage = request.tocList().get(index + 1).page();

        // 다음 페이지가 없거나 잘못된 순서면 무시
        if (nextPage == null || nextPage <= currentPage) return ""; // 다음 페이지가 없거나 잘못된 순서면 공백

        int diff = nextPage - currentPage; // 현재 목차가 차지하는 페이지 수 계산

        return String.format(" (%dp)", diff);
    }

}