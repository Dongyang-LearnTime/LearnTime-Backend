package learntime.backend.domain.study.service.facade;

import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.service.ai.GeminiStudyService;
import learntime.backend.domain.study.service.ai.TocExtractionService;
import learntime.backend.domain.study.service.core.StudyManagementService;
import learntime.backend.domain.study.service.core.StudyQueryService;
import learntime.backend.global.utils.FileValidatorUtil;
import learntime.backend.global.utils.AuthorizationUtil;
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
    private final GeminiStudyService geminiStudyService;
    private final StudyManagementService studyManagementService;

    // 업로드된 이미지에서 AI를 활용하여 목차 정보를 추출합니다.
    public List<TocListResponseDTO> extractToc(MultipartFile imageFile) {
        fileValidatorUtil.validateImage(imageFile);
        return tocExtractionService.extractTocAsJson(imageFile);
    }

    // AI를 활용해 새로운 스마트 학습 계획을 생성하고 저장합니다. (비동기 처리)
    public Long generateAndSaveStudyPlan(GeminiStudyRequestDTO request, Long userId) {
        // 1. 초기 정보 저장 (PLANNING 상태)
        Long studyId = studyManagementService.initializeStudy(request, userId);

        // 2. 비동기로 AI 계획 생성 및 상세 일정 조립 (백그라운드 처리)
        studyManagementService.generateAndSavePlanAsync(studyId, request, userId);

        return studyId;
    }

//    // 특정 스터디와 관련된 모든 데이터를 삭제합니다.
//    @Transactional
//    public void deleteStudy(Long userId) {
//        Study study = studyRepository.findById(studyId)
//                .orElseThrow(() -> new IllegalArgumentException("공부 진도를 찾을 수 없습니다."));
//
//        studyRepository.deleteById(studyId);
//    }

}
