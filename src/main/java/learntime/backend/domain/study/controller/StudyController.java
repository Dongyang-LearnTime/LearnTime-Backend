package learntime.backend.domain.study.controller;

import learntime.backend.domain.study.dto.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.StudyRequestDTO;
import learntime.backend.domain.study.service.GeminiStudyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/study")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // React 연동을 위한 CORS 허용
public class StudyController {

    private final GeminiStudyService studyService;

    @PostMapping("/generate")
    public ResponseEntity<StudyPlanResponseDTO> createPlan(@RequestBody StudyRequestDTO request) {
        StudyPlanResponseDTO result = studyService.generateSmartStudyPlan(request);
        return ResponseEntity.ok(result);
    }
}
