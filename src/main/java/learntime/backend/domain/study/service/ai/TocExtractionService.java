package learntime.backend.domain.study.service.ai;

import jakarta.annotation.PostConstruct;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.code.FileErrorCode;
import learntime.backend.global.error.exception.FileException;
import learntime.backend.global.infra.gemini.GeminiClient;
import learntime.backend.global.utils.GeminiPromptParser;
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

/**
 * AI 이미지 분석(OCR)을 이용한 목차 추출 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TocExtractionService {

    private final GeminiClient geminiClient;
    private final GeminiPromptParser promptParser;

    @Value("classpath:prompts/toc-extract-prompt.txt")
    private Resource promptResource;

    private String promptTemplate;

    // OCR 이미지 분석을 위한 프롬프트 템플릿을 초기화합니다.
    @PostConstruct
    public void init() {
        try {
            this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("OCR 프롬프트 초기화 실패", e);
            throw new StudyException(StudyErrorCode.PROMPT_INIT_FAILED);
        }
    }

    /** 이미지 파일에서 AI(OCR)를 이용해 목차 정보를 추출한다. */
    public List<TocListResponseDTO> extractTocAsJson(MultipartFile imageFile) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Thumbnails.of(imageFile.getInputStream())
                    .size(1600, 1600)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(os);

            byte[] imageBytes = os.toByteArray();
            String mimeType = "image/jpeg";
            
            // 1. Upload file using Gemini File API
            String fileUri = geminiClient.uploadFile(imageBytes, mimeType, imageFile.getOriginalFilename());

            // 2. Build Structured Output Schema
            Map<String, Object> responseSchema = Map.of(
                    "type", "ARRAY",
                    "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "chapter", Map.of("type", "STRING", "description", "The chapter number or name"),
                                    "title", Map.of("type", "STRING", "description", "The title of the section"),
                                    "page", Map.of("type", "INTEGER", "description", "The page number")
                            ),
                            "required", List.of("chapter", "title")
                    )
            );

            // 3. Generate Request Body with File URI and Schema
            Map<String, Object> requestBody = promptParser.createOcrRequestBody(promptTemplate, fileUri, mimeType, responseSchema);
            
            // 4. Send Request
            String rawJson = geminiClient.sendRequest(requestBody, GeminiModel.GEMINI_3_1);

            return promptParser.parseOcrResponse(rawJson);
        } catch (IOException e) {
            log.error("OCR 이미지 파일 리사이징/읽기 실패", e);
            throw new FileException(FileErrorCode.FILE_READ_ERROR);
        } catch (learntime.backend.global.error.exception.BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 목차 추출 실패", e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }
}
