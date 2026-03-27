package learntime.backend.domain.study.controller;

import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.request.SavePlanRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.Yes24BookListResponseDTO;
import learntime.backend.domain.study.service.GeminiStudyService;
import learntime.backend.domain.study.service.StudyCommandService;
import learntime.backend.domain.study.service.StudyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;
    private final GeminiStudyService geminiStudyService;
    private final StudyCommandService studyCommandService;

    // 책 목록 요청
    @GetMapping("/book")
    public ResponseEntity<List<Yes24BookListResponseDTO>> getYes24BookList(@RequestParam("title") String title,
                                                                           @RequestParam("page") int page) {
        List<Yes24BookListResponseDTO> result = studyService.getYes24BookList(title, page);

        return ResponseEntity.ok(result);
    }

    // 책 목록 기반으로 AI가 일정, 진도 생성
    @PostMapping("/generate")
    public ResponseEntity<StudyPlanResponseDTO> createStudyPlan(@Valid @RequestBody GeminiStudyRequestDTO request) {
        StudyPlanResponseDTO result = geminiStudyService.generateSmartStudyPlan(request);

        return ResponseEntity.ok(result);
    }

    // 유저에게 프롬프트를 받아 AI가 진도를 재설정

    // 공부 진도 db에 저장
    @PostMapping("/plans")
    public ResponseEntity<Void> saveStudyPlan(@RequestBody SavePlanRequestDTO request) {
        studyCommandService.saveStudyPlan(request.planInfo(), request.planGeminiResult());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // AI 진도 재조정 (쉬는 날, 쉬는 요일 등 변경)
    @PutMapping("/{studyId}/replan")
    public ResponseEntity<StudyPlanResponseDTO> replanStudyPlan(@PathVariable("studyId") Long studyId,
                                                            @Valid @RequestBody GeminiReplanRequestDTO request) {
        String remainingContent = studyCommandService.getRemainingStudyContent(studyId); // 기존 진행 중인 스터디의 '남은 학습 내용'을 합쳐서 문자열로 가져오기
        
        int remainingDays = studyCommandService.calculateRemainingStudyDays(studyId, request); // 새로 설정된 기간과 휴일을 반영하고 완료된 일수를 제외한 실제 남은 학습 일수 계산
        StudyPlanResponseDTO result = geminiStudyService.generateReplan(request, remainingContent, remainingDays); // 남은 분량과 일수를 AI에게 넘겨서 새로운 일정 생성
        studyCommandService.replanStudy(studyId, request, result); // AI가 짜준 새로운 진도로 기존 스터디 업데이트 (완료된 것은 두고, 남은 것만 교체)

        return ResponseEntity.ok(result);
    }

}
