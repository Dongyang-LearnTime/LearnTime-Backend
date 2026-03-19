package learntime.backend.domain.study.service;

import jakarta.annotation.PostConstruct;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
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
    private Resource promptResource;
    private String promptTemplate;

    @PostConstruct
    public void init() {
        try {
            // 파일 → 메모리 캐싱
            this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("프롬프트 초기화 실패", e);
        }
    }

    public StudyPlanResponseDTO generateSmartStudyPlan(GeminiStudyRequestDTO request) {

        int periodDays = request.getValidatedStudyDays(); // 일차 계산

        String bookToc = yes24BookCrawler.crawlToc(request.getLinkUrl()); // 책 목차 정보 크롤링

        // 프롬프트 생성 및 AI 요청
        String userPrompt = promptTemplate.formatted(periodDays, request.getTitle(), bookToc);
        Map<String, Object> requestBody = promptParser.createRequestBody(userPrompt);

        try {
            String rawJson = geminiClient.sendRequest(requestBody);
            return promptParser.parseResponse(rawJson); // 4. 응답 파싱

        } catch (Exception e) {
            log.error("AI 학습 계획 생성 실패. Request: {}", request, e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }
}