package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.UpdateFeedbackTitleRequestDTO;
import learntime.backend.domain.study.dto.response.StudyFeedbackResponseDTO;
import learntime.backend.domain.study.service.core.StudyFeedbackService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study/feedback")
@RequiredArgsConstructor
@Tag(name = "AI 학습 피드백 API", description = "학습 통계 기반 AI 분석 및 피드백 관리 API")
public class StudyFeedbackController {

    private final StudyFeedbackService studyFeedbackService;

    @PostMapping("/{studyId}/generate")
    @Operation(summary = "AI 피드백 생성", description = "현재 학습 상태를 분석하여 AI 피드백을 생성하고 DB에 저장합니다.")
    public ResponseEntity<StudyFeedbackResponseDTO> generateFeedback(@PathVariable Long studyId,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyFeedbackResponseDTO response = studyFeedbackService.generateAndSaveFeedback(studyId, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{studyId}/list")
    @Operation(summary = "AI 피드백 목록 조회", description = "특정 스터디에 대해 생성된 모든 AI 피드백 목록을 조회합니다.")
    public ResponseEntity<List<StudyFeedbackResponseDTO>> getFeedbackList(@PathVariable Long studyId,
                                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudyFeedbackResponseDTO> response = studyFeedbackService.getFeedbackList(studyId, userDetails.userId());
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/title")
    @Operation(summary = "AI 피드백 제목 수정", description = "생성된 AI 피드백의 제목을 사용자가 수정합니다.")
    public ResponseEntity<Void> updateFeedbackTitle(@Valid @RequestBody UpdateFeedbackTitleRequestDTO request,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyFeedbackService.updateFeedbackTitle(request, userDetails.userId());
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{feedbackId}")
    @Operation(summary = "AI 피드백 삭제", description = "특정 AI 피드백 데이터를 삭제합니다.")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long feedbackId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyFeedbackService.deleteFeedback(feedbackId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

}
