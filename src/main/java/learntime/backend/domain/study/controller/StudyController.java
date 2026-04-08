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
import learntime.backend.global.dto.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

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
    public ResponseEntity<?> extractToc(@RequestParam("image") MultipartFile imageFile) {

        // 1. 실무 최적화: 방어적 프로그래밍 (빈 파일 및 누락 검증)
        if (imageFile == null || imageFile.isEmpty()) {
            System.out.println("[TOC Extract] 업로드된 이미지 파일이 없습니다.");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "이미지 파일을 첨부해주세요."));
        }

        // 2. MIME 타입 검증 (보안: 이미지 이외의 악성 스크립트 파일 업로드 방지)
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            System.out.println("[TOC Extract] 잘못된 파일 형식 요청: {}" + contentType);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "이미지 파일(jpg, png 등)만 업로드 가능합니다."));
        }

        try {
            // 3. Service 계층 위임 (제미나이 API 호출)
            String jsonResult = tocExtractionService.extractTocAsJson(imageFile);

            // 4. RESTful 원칙에 따른 응답.
            // Service에서 파싱된 '순수 JSON 문자열'을 반환하므로 Content-Type을 application/json으로 강제 지정
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonResult);

        } catch (Exception e) {
            System.out.println("[TOC Extract] 서버 내부 처리 중 예외 발생" + e);
            // 실무에서는 @RestControllerAdvice 기반의 GlobalExceptionHandler로 이관하는 것이 유지보수에 유리합니다.
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "이미지 분석 중 서버 오류가 발생했습니다."));
        }
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
