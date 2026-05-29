package learntime.backend.domain.study.service.facade;

import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.request.UpdateStudyRestScheduleRequestDTO;
import learntime.backend.domain.study.dto.request.UpdateStudyTitleRequestDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.domain.study.service.ai.TocExtractionService;
import learntime.backend.domain.study.service.core.StudyInitializationService;
import learntime.backend.domain.study.service.core.StudyManagementService;
import learntime.backend.domain.study.service.core.StudyPlanGenerationService;
import learntime.backend.domain.study.service.core.StudyRestService;
import learntime.backend.global.utils.FileValidatorUtil;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// AI 서비스와 DB 핵심 서비스를 조합하여 컨트롤러에 고수준의 기능을 제공하는 Facade
@Component
@RequiredArgsConstructor
public class StudyFacade {

    private final FileValidatorUtil fileValidatorUtil;
    private final TocExtractionService tocExtractionService;
    private final StudyInitializationService studyInitializationService;
    private final StudyPlanGenerationService studyPlanGenerationService;
    private final StudyRestService studyRestService;
    private final StudyManagementService studyManagementService;
    private final PromptQuotaUtil promptQuotaUtil;

    // 업로드된 이미지에서 AI를 활용하여 목차 정보를 추출합니다.
    public List<TocListResponseDTO> extractToc(MultipartFile imageFile, Long userId) {
        fileValidatorUtil.validateImage(imageFile);
        promptQuotaUtil.decreasePromptQuota(userId); // Gemini 이용량 차감
        try {
            return tocExtractionService.extractTocAsJson(imageFile);
        } catch (Exception e) {
            promptQuotaUtil.restorePromptQuota(userId); // 예외 발생 시 할당량 복구
            throw e;
        }
    }

    // AI를 활용해 새로운 스마트 학습 계획을 생성하고 저장합니다. (비동기 처리)
    public Long generateAndSaveStudyPlan(GeminiStudyRequestDTO request, Long userId) {
        // 1. 초기 정보 저장 (PLANNING 상태)
        Long studyId = studyInitializationService.initializeStudy(request, userId);

        // 2. 비동기로 AI 계획 생성 및 상세 일정 조립 (백그라운드 처리)
        studyPlanGenerationService.generateAndSavePlanAsync(studyId, request, userId);

        return studyId;
    }

    // 공부 제목 변경
    @Transactional
    public void updateTitle(UpdateStudyTitleRequestDTO request, Long userId, boolean isStudyTitle) {
        studyManagementService.updateTitle(request, userId, isStudyTitle);
    }

    // 휴무 요일/날짜를 재조정하고, 미래 공부 일정의 날짜만 다시 배치합니다.
    @Transactional
    public void updateRestSchedule(Long studyId, UpdateStudyRestScheduleRequestDTO request, Long userId) {
        studyRestService.updateRestSchedule(studyId, request, userId);
    }

    // 특정 스터디와 관련된 모든 데이터를 삭제합니다.
    @Transactional
    public void deleteStudy(Long studyId, Long userId) {
        studyManagementService.deleteStudyBulk(studyId, userId);
    }

}
