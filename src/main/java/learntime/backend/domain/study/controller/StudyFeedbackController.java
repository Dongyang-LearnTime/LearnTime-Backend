package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.UpdateFeedbackTitleRequestDTO;
import learntime.backend.domain.study.dto.response.StudyFeedbackResponseDTO;
import learntime.backend.domain.study.service.core.StudyFeedbackService;
import learntime.backend.global.security.CustomUserDetails;
import learntime.backend.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study/feedback")
@RequiredArgsConstructor
@Tag(name = "학습 피드백 API", description = "AI 기반 학습 피드백 생성 및 관리 API")
public class StudyFeedbackController {

    private final StudyFeedbackService studyFeedbackService;

    @PostMapping("/{studyId}")
    @Operation(summary = "AI 학습 피드백 생성", description = "최근 학습 데이터를 바탕으로 AI 피드백을 생성하고 저장합니다.")
    public ResponseEntity<StudyFeedbackResponseDTO> generateFeedback(@PathVariable Long studyId,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyFeedbackResponseDTO result =
                studyFeedbackService.generateAndSaveFeedback(studyId, userDetails.userId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list/{studyId}")
    @Operation(summary = "피드백 목록 조회", description = "특정 스터디의 모든 피드백 기록을 조회합니다. (오프셋 페이징)")
    public ResponseEntity<PageResponse<StudyFeedbackResponseDTO>> getFeedbackList(
            @PathVariable Long studyId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageResponse<StudyFeedbackResponseDTO> response =
                studyFeedbackService.getMemberFeedbacks(studyId, pageable, userDetails.userId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/title")
    @Operation(summary = "피드백 제목 수정", description = "생성된 피드백의 제목을 수정합니다.")
    public ResponseEntity<Void> updateFeedbackTitle(@Valid @RequestBody UpdateFeedbackTitleRequestDTO request,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyFeedbackService.updateFeedbackTitle(request, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{feedbackId}")
    @Operation(summary = "피드백 삭제", description = "특정 피드백 기록을 삭제합니다.")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long feedbackId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyFeedbackService.deleteFeedback(feedbackId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }
}
