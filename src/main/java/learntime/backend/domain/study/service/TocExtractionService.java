package learntime.backend.domain.study.service;

import jakarta.annotation.PostConstruct;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.domain.study.service.gemini.GeminiPromptParser;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.infra.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import net.coobird.thumbnailator.Thumbnails;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TocExtractionService {

    private final GeminiClient geminiClient;
    private final GeminiPromptParser promptParser;

    // 프롬프트 폴더에서 OCR 지시문 주입
    @Value("classpath:prompts/toc-extract-prompt.txt")
    private Resource promptResource;

    private String promptTemplate;

    @PostConstruct
    public void init() {
        try {
            this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("OCR 프롬프트 초기화 실패", e);
        }
    }

    public List<TocListResponseDTO> extractTocAsJson(MultipartFile imageFile) {
        try {
            // 메모리 최적화: MultipartFile의 Stream을 바로 읽어 리사이징 후 ByteArrayOutputStream에 저장
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Thumbnails.of(imageFile.getInputStream())
                    .size(1024, 1024) // Gemini Vision 처리 권장 해상도 수준으로 축소
                    .outputFormat("jpg")
                    .outputQuality(0.7) // 품질 70% 압축
                    .toOutputStream(os);

            // Base64 인코딩: 압축된 바이트 배열을 사용하여 페이로드 크기 대폭 감소
            String base64Image = Base64.getEncoder().encodeToString(os.toByteArray());
            String mimeType = "image/jpeg"; // 압축 포맷 고정

            Map<String, Object> requestBody = promptParser.createOcrRequestBody(promptTemplate, base64Image, mimeType);
            String rawJson = geminiClient.sendRequest(requestBody, GeminiModel.GEMINI_3_1);

            return promptParser.parseOcrResponse(rawJson);
        } catch (IOException e) {
            log.error("OCR 이미지 파일 리사이징/읽기 실패", e);
            throw new RuntimeException("이미지 파일 처리 실패");
        } catch (Exception e) {
            log.error("AI 목차 추출 실패", e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }
}