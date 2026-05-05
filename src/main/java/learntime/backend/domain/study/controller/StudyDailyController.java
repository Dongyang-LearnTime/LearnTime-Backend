package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.PlanCompleteRequestDTO;
import learntime.backend.domain.study.service.core.StudyDailyService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 일일 학습 진도 완료 및 체크 관련 API
 */
@RestController
@RequestMapping("/api/study/daily")
@RequiredArgsConstructor
@Tag(name = "일일 진도 API", description = "학습 완료 체크 및 포인트 지급 관련 API (JWT 필요)")
public class StudyDailyController {

    private final StudyDailyService studyDailyService;

    @PatchMapping("/completion")
    @Operation(summary = "일일 진도 완료", description = "공부 일일진도를 완료로 변경하고 포인트를 지급합니다.")
    public ResponseEntity<String> completePlan(@Valid @RequestBody PlanCompleteRequestDTO request,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        int resultPoint = studyDailyService.completeStudyDailyPlan(request, userDetails.userId());
        return ResponseEntity.ok("포인트 " + resultPoint + "지급 완료!");
    }
}
