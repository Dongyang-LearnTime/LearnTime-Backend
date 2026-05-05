package learntime.backend.domain.study.service.facade;

import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.domain.study.service.ai.GeminiStudyService;
import learntime.backend.domain.study.service.ai.TocExtractionService;
import learntime.backend.domain.study.service.core.StudyCoreService;
import learntime.backend.domain.study.service.util.StudyFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// AI 서비스와 DB 핵심 서비스를 조합하여 컨트롤러에 고수준의 기능을 제공하는 Facade
@Component
@RequiredArgsConstructor
public class StudyFacade {

    private final StudyFileValidator studyFileValidator;
    private final TocExtractionService tocExtractionService;
    private final GeminiStudyService geminiStudyService;
    private final StudyCoreService studyCoreService;

    /**
     * 사진 목차 추출
     */
    public List<TocListResponseDTO> extractToc(MultipartFile imageFile) {
        studyFileValidator.validateImage(imageFile);
        return tocExtractionService.extractTocAsJson(imageFile);
    }

    /**
     * 신규 학습 계획 생성 및 저장
     */
    public void generateAndSaveStudyPlan(GeminiStudyRequestDTO request, Long userId) {
        StudyPlanResponseDTO geminiResult = geminiStudyService.generateSmartStudyPlan(request, userId);
        studyCoreService.saveStudyPlan(request, geminiResult, userId);
    }

    /**
     * 기존 학습 계획 재설계 및 업데이트
     */
    public StudyPlanResponseDTO replanAndSaveStudy(Long studyId, GeminiReplanRequestDTO request, Long userId) {
        String remainingContent = studyCoreService.getRemainingStudyContent(studyId);
        int remainingDays = studyCoreService.calculateRemainingStudyDays(studyId, request);

        StudyPlanResponseDTO result = geminiStudyService.generateReplan(request, remainingContent, remainingDays, userId);
        studyCoreService.replanStudy(studyId, request, result, userId);

        return result;
    }
}
