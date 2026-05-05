package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.StudyNoteRequestDTO;
import learntime.backend.domain.study.dto.request.StudyNotesUpdateRequestDTO;
import learntime.backend.domain.study.dto.response.StudyNotesResponseDTO;
import learntime.backend.domain.study.service.core.StudyNotesService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study/notes")
@RequiredArgsConstructor
@Tag(name = "공부 필기 API", description = "공부 필기(Notes) 관련 CRUD API (JWT 필요)")
public class StudyNotesController {

    private final StudyNotesService studyNotesService;

    @GetMapping("/{studyNotesId}")
    @Operation(summary = "공부 필기 단건 조회", description = "특정 공부 필기를 조회합니다.")
    public ResponseEntity<StudyNotesResponseDTO> getStudyNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studyNotesId) {
        StudyNotesResponseDTO response = studyNotesService.getNote(studyNotesId, userDetails.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/study/{studyId}")
    @Operation(summary = "공부 ID로 필기 목록 조회", description = "특정 공부(Study)에 속한 모든 필기 목록을 조회합니다.")
    public ResponseEntity<List<StudyNotesResponseDTO>> getStudyNotesByStudyId(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studyId) {
        List<StudyNotesResponseDTO> responses = studyNotesService.getNotesByStudyId(studyId, userDetails.userId());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @Operation(summary = "공부 필기 저장", description = "새로운 공부 필기를 저장하고 ID를 반환합니다.")
    public ResponseEntity<Long> saveStudyNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StudyNoteRequestDTO request) {
        Long studyNotesId = studyNotesService.create(request, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(studyNotesId);
    }

    @PutMapping("/{studyNotesId}")
    @Operation(summary = "공부 필기 수정", description = "특정 공부 필기를 수정합니다.")
    public ResponseEntity<Void> updateStudyNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studyNotesId,
            @Valid @RequestBody StudyNotesUpdateRequestDTO request) {
        studyNotesService.update(studyNotesId, request, userDetails.userId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{studyNotesId}")
    @Operation(summary = "공부 필기 삭제", description = "특정 공부 필기를 삭제합니다.")
    public ResponseEntity<Void> deleteStudyNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studyNotesId) {
        studyNotesService.delete(studyNotesId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

}
