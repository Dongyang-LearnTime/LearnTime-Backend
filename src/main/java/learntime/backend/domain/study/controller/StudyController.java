package learntime.backend.domain.study.controller;

import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.request.SavePlanRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.service.GeminiStudyService;
import learntime.backend.domain.study.service.StudyCommandService;
import learntime.backend.domain.study.service.StudyService;
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

@Slf4j
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final FileValidator fileValidator;
    private final StudyService studyService;
    private final GeminiStudyService geminiStudyService;
    private final StudyCommandService studyCommandService;
    private final TocExtractionService tocExtractionService;

//    // 책 목록 요청
//    @GetMapping("/book")
//    public ResponseEntity<List<Yes24BookListResponseDTO>> getYes24BookList(@RequestParam("title") String title,
//                                                                           @RequestParam("page") int page) {
//        List<Yes24BookListResponseDTO> result = studyService.getYes24BookList(title, page);
//
//        return ResponseEntity.ok(result);
//    }


    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> extractToc(@RequestParam("image") MultipartFile imageFile) {

        fileValidator.validateImage(imageFile); // 이미지 파일 검사
        log.info("[TOC Extract] 파일 검증 완료: {}", imageFile.getOriginalFilename());

        String jsonResult = tocExtractionService.extractTocAsJson(imageFile);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonResult);
    }

    // 책 정보 기반으로 AI가 일정, 진도 생성
    @PostMapping("/generate")
    public ResponseEntity<StudyPlanResponseDTO> createStudyPlan(@Valid @RequestBody GeminiStudyRequestDTO request,
                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyPlanResponseDTO result = geminiStudyService.generateSmartStudyPlan(request, userDetails.userId());

        return ResponseEntity.ok(result);
    }

    // 공부 진도 db에 저장
    @PostMapping("/save")
    public ResponseEntity<Void> saveStudyPlan(@RequestBody SavePlanRequestDTO request,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        studyCommandService.saveStudyPlan(request.planInfo(), request.planGeminiResult(), userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // AI 진도 재조정 (쉬는 날, 쉬는 요일 등 변경)
    @PutMapping("/{studyId}/replan")
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
