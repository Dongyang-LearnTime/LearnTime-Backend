package learntime.backend.domain.study.service;

import jakarta.annotation.PostConstruct;
import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.Yes24BookInfoResponseDTO;
import learntime.backend.domain.study.service.component.Yes24BookCrawler;
import learntime.backend.domain.study.service.component.GeminiPromptParser;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;
import learntime.backend.global.infra.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiStudyService {

    private final GeminiClient geminiClient;
    private final Yes24BookCrawler yes24BookCrawler;
    private final GeminiPromptParser promptParser;

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

    public StudyPlanResponseDTO generateSmartStudyPlan(GeminiStudyRequestDTO request) {
        int periodDays = request.getValidatedStudyDays();

        Yes24BookInfoResponseDTO crawlingResult =
                yes24BookCrawler.crawlToc(request.linkUrl());

        String userPrompt = promptTemplate.formatted(
                periodDays,
                request.bookTitle(),
                crawlingResult.pageCount(),
                crawlingResult.bookToc()
        );

        return executeGeminiRequest(userPrompt, request);
    }

    public StudyPlanResponseDTO generateReplan(GeminiReplanRequestDTO request, String remainingContent, int remainingDays) {
        String userPrompt = replanPromptTemplate.formatted(
                remainingDays,
                request.studyTitle(),
                remainingContent
        );

        return executeGeminiRequest(userPrompt, request);
    }

    // 프롬프트를 넣어 결과를 파싱하여 반환
    private StudyPlanResponseDTO executeGeminiRequest(String userPrompt, Object requestDto) {
        Map<String, Object> requestBody = promptParser.createRequestBody(userPrompt);

        try {
            String rawJson = geminiClient.sendRequest(requestBody);
            return promptParser.parseResponse(rawJson);  // JSON (String) -> DTO 변환 과정
        } catch (Exception e) {
            log.error("AI 학습 계획 생성 실패. Request: {}", requestDto, e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

}