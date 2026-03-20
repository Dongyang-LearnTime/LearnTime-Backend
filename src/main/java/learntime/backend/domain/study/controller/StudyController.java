package learntime.backend.domain.study.controller;

import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.Yes24BookListResponseDTO;
import learntime.backend.domain.study.service.GeminiStudyService;
import learntime.backend.domain.study.service.StudyService;
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

    // 책 목록 요청
    @GetMapping("/book")
    public ResponseEntity<List<Yes24BookListResponseDTO>> getYes24BookList(@RequestParam("title") String title,
                                                                           @RequestParam("page") int page) {
        List<Yes24BookListResponseDTO> result = studyService.getYes24BookList(title, page);
        return ResponseEntity.ok(result);
    }

    // 책 목록 기반으로 AI가 일정, 진도 생성
    @PostMapping("/generate")
    public ResponseEntity<StudyPlanResponseDTO> createPlan(@Valid @RequestBody GeminiStudyRequestDTO request) {
        StudyPlanResponseDTO result = geminiStudyService.generateSmartStudyPlan(request);
        return ResponseEntity.ok(result);
    }
}
