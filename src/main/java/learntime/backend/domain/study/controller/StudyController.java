package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study_plan.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study_plan.dto.request.UpdateStudyRestScheduleRequestDTO;
import learntime.backend.domain.study.dto.request.UpdateStudyTitleRequestDTO;
import learntime.backend.domain.study.dto.request.UpdateStudyVisibilityRequestDTO;
import learntime.backend.domain.study_progress.dto.response.StudyMemberRecentWeekInfoResponseDTO;
import learntime.backend.domain.study_progress.dto.response.StudyProgressIndicatorResponseDTO;
import learntime.backend.domain.study.dto.response.StudyStatusResponseDTO;
import learntime.backend.domain.study_progress.dto.response.StudyStudioSummaryResponseDTO;
import learntime.backend.domain.study_progress.dto.response.StudyTotalInfoResponseDTO;
import learntime.backend.domain.study_plan.dto.response.TocListResponseDTO;
import learntime.backend.domain.study_progress.service.StudyDailyService;
import learntime.backend.domain.study_progress.service.StudyUserContentService;
import learntime.backend.domain.study.service.facade.StudyFacade;
import learntime.backend.domain.study_progress.service.StudyQueryService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
@Tag(name = "공부 진도 API", description = "AI를 활용한 진도 생성, 수정 및 관리 API (JWT 필요)")
public class StudyController {

    private final StudyFacade studyFacade;
    private final StudyQueryService studyQueryService;
    private final StudyDailyService studyDailyService;
    private final StudyUserContentService studyUserContentService;

    @GetMapping("/progress")
    @Operation(
            summary = "나의 전체 공부 진도 및 오늘 계획 여부 조회",
            description = "내가 진행 중인 모든 공부 진도(Study)의 ID 목록과 각 진도별 오늘 계획 존재 여부를 조회합니다."
    )
    public ResponseEntity<List<StudyProgressIndicatorResponseDTO>> getMyStudyProgresses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudyProgressIndicatorResponseDTO> result = studyQueryService.getMyStudyProgresses(userDetails.userId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{studyId}/total")
    @Operation(
            summary = "공부 핵심 지표",
            description = "진도 달성률, 퀴즈 정답률, 집중 시간 등 공부의 핵심 지표를 조회합니다.")
    public ResponseEntity<StudyTotalInfoResponseDTO> totalStudyIndicator(@PathVariable Long studyId,
                                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyTotalInfoResponseDTO result = studyQueryService.getStudyMemberTotalIndicatorByUserId(studyId, userDetails.userId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{studyId}/total/recent-week")
    @Operation(
            summary = "최근 일주일 공부 상태",
            description = "오늘을 제외한 최근 7일의 날짜별 집중 시간, 진행 상태, 완료 상태, 이해도 점수를 조회합니다.")
    public ResponseEntity<List<StudyMemberRecentWeekInfoResponseDTO>> recentWeekStudyIndicator(@PathVariable Long studyId,
                                                                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudyMemberRecentWeekInfoResponseDTO> result = studyQueryService.getRecentWeekStudyInfos(studyId, userDetails.userId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{studyId}/studio-summary")
    @Operation(
            summary = "학습 스튜디오 첫 화면 통합 조회",
            description = "첫 화면에 필요한 오늘의 진도, 사용자 작성 내용, 핵심 지표, 최근 일주일 지표를 한 번에 조회합니다."
    )
    public ResponseEntity<StudyStudioSummaryResponseDTO> getStudioSummary(
            @PathVariable Long studyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyStudioSummaryResponseDTO result = StudyStudioSummaryResponseDTO.builder()
                .todayPlan(studyDailyService.getStudyPlanInfoByDate(studyId, planDate, userDetails.userId()))
                .todayContent(studyUserContentService.getUserContents(studyId, userDetails.userId(), planDate))
                .totalIndicator(studyQueryService.getStudyMemberTotalIndicatorByUserId(studyId, userDetails.userId()))
                .recentWeekIndicator(studyQueryService.getRecentWeekStudyInfos(studyId, userDetails.userId()))
                .build();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{studyId}/status")
    @Operation(summary = "공부 진도 생성 상태 확인", description = "스터디의 현재 생성 상태(PLANNING/READY/FAILED)를 확인합니다.")
    public ResponseEntity<StudyStatusResponseDTO> getStudyStatus(@PathVariable Long studyId,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(studyQueryService.getStudyStatus(studyId, userDetails.userId()));
    }

    @PostMapping(
            value = "/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "사진 인식(목차 추출)", description = "사진 파일을 받아 AI를 통해 목차 정보를 추출합니다.")
    public ResponseEntity<List<TocListResponseDTO>> extractToc(
            @RequestParam("image") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("[TOC Extract] 목차 추출 요청: {}", imageFile.getOriginalFilename());
        List<TocListResponseDTO> jsonResult = studyFacade.extractToc(imageFile, userDetails.userId());

        return ResponseEntity.ok(jsonResult);
    }

    @PostMapping("/generate")
    @Operation(summary = "공부 진도 생성", description = "목차 정보를 기반으로 공부 진도를 생성 후 DB에 저장합니다.")
    public ResponseEntity<Long> createStudyPlan(@Valid @RequestBody GeminiStudyRequestDTO request,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long studyId = studyFacade.generateAndSaveStudyPlan(request, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(studyId);
    }

    @PatchMapping("/study-title")
    @Operation(summary = "공부 진도 제목 변경", description = "공부 진도의 제목을 변경합니다.")
    public ResponseEntity<Void> changeStudyTitle(@Valid @RequestBody UpdateStudyTitleRequestDTO request,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isStudyTitle = true; // 공부 진도 제목 여부
        studyFacade.updateTitle(request, userDetails.userId(), isStudyTitle);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/book-title")
    @Operation(summary = "책 제목 변경", description = "공부 진도의 책 제목을 변경합니다.")
    public ResponseEntity<Void> changeStudyBookTitle(@Valid @RequestBody UpdateStudyTitleRequestDTO request,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isStudyTitle = false; // 공부 진도 제목 여부
        studyFacade.updateTitle(request, userDetails.userId(), isStudyTitle);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{studyId}/visibility")
    @Operation(summary = "스터디 공개/비공개 설정 변경", description = "방장이 스터디의 공개/비공개 상태를 변경합니다.")
    public ResponseEntity<Void> updateStudyVisibility(
            @PathVariable Long studyId,
            @Valid @RequestBody UpdateStudyVisibilityRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyFacade.updateStudyVisibility(studyId, request.isPublic(), userDetails.userId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{studyId}/rests")
    @Operation(
            summary = "공부 휴무 일정 재조정",
            description = "휴무 요일/날짜를 변경하며, 기존 공부 내용은 유지한 채 오늘 이후 일정 날짜만 재배치합니다."
    )
    public ResponseEntity<Void> updateStudyRestSchedule(@PathVariable Long studyId,
                                                        @Valid @RequestBody UpdateStudyRestScheduleRequestDTO request,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyFacade.updateRestSchedule(studyId, request, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{studyId}")
    @Operation(summary = "공부 진도 삭제", description = "특정 공부 진도 계획을 삭제합니다.")
    public ResponseEntity<Void> deleteStudy(@PathVariable Long studyId,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyFacade.deleteStudy(studyId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }


//    @PutMapping("/{studyId}/replan")
//    @Operation(summary = "공부 진도 재생성", description = "AI를 사용하여 기존 진도를 재조정합니다.")
//    public ResponseEntity<StudyPlanResponseDTO> replanStudyPlan(@PathVariable Long studyId,
//                                                                @Valid @RequestBody GeminiReplanRequestDTO request,
//                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
//        StudyPlanResponseDTO result = studyFacade.replanAndSaveStudy(studyId, request, userDetails.userId());
//        return ResponseEntity.ok(result);
//    }

}
