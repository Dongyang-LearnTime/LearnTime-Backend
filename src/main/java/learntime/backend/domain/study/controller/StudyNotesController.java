package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.StudyNoteRequestDTO;
import learntime.backend.domain.study.service.StudyNotesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study/notes")
@RequiredArgsConstructor
@Tag(name = "공부 필기 API", description = "공부 필기 관련 API임 (JWT 필요)")
public class StudyNotesController {

    private final StudyNotesService studyNotesService;

    // 공부 필기 조회 시 SET NULL에 따른 NULL 고려

    @PostMapping
    @Operation(summary = "공부 필기 저장", description = "공부 필기를 저장")
    public ResponseEntity<Void> saveStudyNotes(@Valid @RequestBody StudyNoteRequestDTO request) {
        studyNotesService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }



}
