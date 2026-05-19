package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.PlanCompleteRequestDTO;
import learntime.backend.domain.study.dto.request.StudyUserContentRequestDTO;
import learntime.backend.domain.study.dto.response.StudyDailyPlanInfoResponseDTO;
import learntime.backend.domain.study.service.core.StudyDailyService;
import learntime.backend.domain.study.service.core.StudyUserContentService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/study/daily")
@RequiredArgsConstructor
@Tag(name = "일일 진도 API", description = "학습 완료 체크 및 포인트 지급 관련 API (JWT 필요)")
public class StudyDailyController {

    private final StudyDailyService studyDailyService;
    private final StudyUserContentService studyUserContentService;

    @GetMapping("/{studyId}")
    @Operation(summary = "특정 날짜 공부 계획 조회", description = "planDate를 기준으로 특정 스터디의 휴무 여부 및 일일 학습 계획을 조회합니다.")
    public ResponseEntity<StudyDailyPlanInfoResponseDTO> getStudyPlanInfoByDate(
            @PathVariable Long studyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyDailyPlanInfoResponseDTO response = studyDailyService.getStudyPlanInfoByDate(studyId, planDate, userDetails.userId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/completion")
    @Operation(summary = "일일 진도 완료", description = "공부 일일진도를 완료로 변경하고 포인트를 지급합니다.")
    public ResponseEntity<String> completePlan(@Valid @RequestBody PlanCompleteRequestDTO request,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        int resultPoint = studyDailyService.completeStudyDailyPlan(request, userDetails.userId());
        return ResponseEntity.ok("포인트 " + resultPoint + "지급 완료!");
    }

    @PostMapping("/content")
    @Operation(summary = "일일 진도 내용 추가/수정",
            description = "특정 일차의 공부 내용을 추가하거나 수정합니다. 저장 시 해당 계획의 상태가 '진행 중'으로 변경됩니다.")
    public ResponseEntity<Long> upsertUserContent(@Valid @RequestBody StudyUserContentRequestDTO request,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long studyUserContentId = studyUserContentService.upsertUserContent(request, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(studyUserContentId);
    }

}
