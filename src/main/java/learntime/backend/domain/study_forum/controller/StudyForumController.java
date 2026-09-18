package learntime.backend.domain.study_forum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import learntime.backend.domain.study_forum.dto.request.StudyForumMessageRequestDTO;
import learntime.backend.domain.study_forum.dto.response.StudyForumMessageResponseDTO;
import learntime.backend.domain.study_forum.service.StudyForumService;
import learntime.backend.global.dto.CursorResponse;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study/{studyId}/forum/messages")
@RequiredArgsConstructor
@Tag(name = "스터디 토론방", description = "스터디 멤버 전용 텍스트 메시지")
public class StudyForumController {
    private final StudyForumService service;

    @GetMapping
    @Operation(summary = "토론 메시지 조회", description = "최신순 커서 조회. ACTIVE/COMPLETED만 허용하며 양방향 차단 사용자의 메시지는 제외합니다.")
    public CursorResponse<StudyForumMessageResponseDTO> getMessages(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable @Positive Long studyId,
            @RequestParam(required = false) @Positive Long beforeId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) Integer size) {
        return service.getMessages(studyId, user.userId(), beforeId, size);
    }

    @PostMapping
    @Operation(summary = "토론 메시지 작성", description = "ACTIVE 멤버만 작성할 수 있습니다. 본문은 최대 1,000자입니다.")
    public ResponseEntity<StudyForumMessageResponseDTO> create(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable @Positive Long studyId,
            @Valid @RequestBody StudyForumMessageRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(studyId, user.userId(), request));
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "본인 토론 메시지 삭제", description = "ACTIVE/COMPLETED 작성자만 영구 삭제할 수 있습니다.")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable @Positive Long studyId,
            @PathVariable @Positive Long messageId) {
        service.delete(studyId, messageId, user.userId());
        return ResponseEntity.noContent().build();
    }
}

