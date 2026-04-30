package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.domain.study.service.GeminiStudyService;
import learntime.backend.domain.study.service.StudyCommandService;
import learntime.backend.domain.study.service.TocExtractionService;
import learntime.backend.domain.study.service.component.FileValidator;
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
@Tag(name = "공부 진도 API", description = "AI를 활용한 진도 생성, 수정 등의 API임 (JWT 필요)")
public class StudyController {

    private final FileValidator fileValidator;
    private final GeminiStudyService geminiStudyService;
    private final StudyCommandService studyCommandService;
    private final TocExtractionService tocExtractionService;

    @PostMapping(
            value = "/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE //  multipart/form-data 요청만 받음
    )
    @Operation(summary = "사진 인식", description = "사진 파일을 받아 AI를 호출하여 목차 정보를 추출함.")
    public ResponseEntity<List<TocListResponseDTO>> extractToc(
            @RequestParam("image") MultipartFile imageFile) { // image라는 이름의 파일만 받음

        fileValidator.validateImage(imageFile); // 이미지 파일 검사
        log.info("[TOC Extract] 파일 검증 완료: {}", imageFile.getOriginalFilename());

        List<TocListResponseDTO> jsonResult = tocExtractionService.extractTocAsJson(imageFile);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonResult);
    }


    @PostMapping("/generate")
    @Operation(summary = "공부 진도 생성", description = "목차 정보를 기반으로 공부 진도를 생성 후 DB에 저장함.")
    public ResponseEntity<StudyPlanResponseDTO> createStudyPlan(@Valid @RequestBody GeminiStudyRequestDTO request,
                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyPlanResponseDTO geminiResult =
                geminiStudyService.generateSmartStudyPlan(request, userDetails.userId());
        studyCommandService.
                saveStudyPlan(request, geminiResult, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PutMapping("/{studyId}/replan")
    @Operation(summary = "공부 진도 재생성", description = "AI 진도 재조정 (쉬는 날, 쉬는 요일 등 변경)")
    public ResponseEntity<StudyPlanResponseDTO> replanStudyPlan(@PathVariable Long studyId,
                                                                @Valid @RequestBody GeminiReplanRequestDTO request,
                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        String remainingContent = studyCommandService.getRemainingStudyContent(studyId); // 기존 진행 중인 스터디의 '남은 학습 내용'을 합쳐서 문자열로 가져오기
        int remainingDays = studyCommandService.calculateRemainingStudyDays(studyId, request); // 새로 설정된 기간과 휴일을 반영하고 완료된 일수를 제외한 실제 남은 학습 일수 계산

        StudyPlanResponseDTO result =
                geminiStudyService.generateReplan(request, remainingContent, remainingDays, userDetails.userId());
        studyCommandService.replanStudy(studyId, request, result, userDetails.userId()); // AI가 짜준 새로운 진도로 기존 스터디 업데이트

        return ResponseEntity.ok(result);
    }

}
