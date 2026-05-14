package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.request.StudyResetRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.StudyRecentWeekInfoResponseDTO;
import learntime.backend.domain.study.dto.response.StudyTotalInfoResponseDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.domain.study.service.facade.StudyFacade;
import learntime.backend.domain.study.service.core.StudyManagementService;
import learntime.backend.domain.study.service.core.StudyQueryService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
@Tag(name = "공부 진도 API", description = "AI를 활용한 진도 생성, 수정 및 관리 API (JWT 필요)")
public class StudyController {

    private final StudyFacade studyFacade;
    private final StudyQueryService studyQueryService;
    private final StudyManagementService studyManagementService;

    @GetMapping("/{studyId}/total")
    @Operation(
            summary = "공부 핵심 지표",
            description = "진도 달성률, 퀴즈 정답률, 집중 시간 등 공부의 핵심 지표를 조회합니다. (캐싱함)")
    public ResponseEntity<StudyTotalInfoResponseDTO> totalStudyIndicator(@PathVariable Long studyId) {
        StudyTotalInfoResponseDTO result = studyQueryService.getStudyTotalIndicator(studyId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{studyId}/total/recent-week")
    @Operation(
            summary = "최근 일주일 공부 상태",
            description = "오늘을 제외한 최근 7일의 날짜별 집중 시간, 진행 상태, 완료 상태, 이해도 점수를 조회합니다.")
    public ResponseEntity<List<StudyRecentWeekInfoResponseDTO>> recentWeekStudyIndicator(@PathVariable Long studyId) {
        List<StudyRecentWeekInfoResponseDTO> result = studyQueryService.getRecentWeekStudyInfos(studyId);
        return ResponseEntity.ok(result);
    }


    @PostMapping(
            value = "/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "사진 인식(목차 추출)", description = "사진 파일을 받아 AI를 통해 목차 정보를 추출합니다.")
    public ResponseEntity<List<TocListResponseDTO>> extractToc(
            @RequestParam("image") MultipartFile imageFile) {

        log.info("[TOC Extract] 목차 추출 요청: {}", imageFile.getOriginalFilename());
        List<TocListResponseDTO> jsonResult = studyFacade.extractToc(imageFile);

        return ResponseEntity.ok(jsonResult);
    }


    @PostMapping("/generate")
    @Operation(summary = "공부 진도 생성", description = "목차 정보를 기반으로 공부 진도를 생성 후 DB에 저장합니다.")
    public ResponseEntity<Void> createStudyPlan(@Valid @RequestBody GeminiStudyRequestDTO request,
                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyFacade.generateAndSaveStudyPlan(request, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PutMapping("/{studyId}/replan")
    @Operation(summary = "공부 진도 재생성", description = "AI를 사용하여 기존 진도를 재조정합니다.")
    public ResponseEntity<StudyPlanResponseDTO> replanStudyPlan(@PathVariable Long studyId,
                                                                @Valid @RequestBody GeminiReplanRequestDTO request,
                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyPlanResponseDTO result = studyFacade.replanAndSaveStudy(studyId, request, userDetails.userId());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{studyId}/reset")
    @Operation(summary = "공부 진도 초기화", description = "새로운 시작일과 휴일 정보를 바탕으로 공부 진도를 초기화합니다.")
    public ResponseEntity<Void> resetStudy(@PathVariable Long studyId,
                                           @Valid @RequestBody StudyResetRequestDTO request,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyManagementService.resetStudy(studyId, request, userDetails.userId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{studyId}")
    @Operation(summary = "공부 진도 삭제", description = "특정 공부 진도 계획을 삭제합니다.")
    public ResponseEntity<Void> deleteStudy(@PathVariable Long studyId,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyFacade.deleteStudy(studyId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

}
