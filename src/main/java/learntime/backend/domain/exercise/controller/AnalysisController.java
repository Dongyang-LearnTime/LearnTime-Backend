package learntime.backend.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.exercise.dto.response.AnalysisResponseDTO;
import learntime.backend.domain.exercise.service.AnalysisService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercise/analysis") // 인증된 사용자 전용 경로
@RequiredArgsConstructor
@Tag(name = "운동 분석 API", description = "사용자의 운동 기록을 분석하고, Gemini가 운동 습관 및 식단 관련 조언 생성")
public class AnalysisController {

    private final AnalysisService analysisService;

    // 최근 7일 건강 데이터를 바탕으로 AI 분석 제안 조회
    @GetMapping("/weekly")
    @Operation(summary = "운동 조언 생성", description = "최근 1주간의 운동 기록을 분석하여, 적절한 조언 생성")
    public ResponseEntity<AnalysisResponseDTO> getWeeklyAnalysis(@AuthenticationPrincipal CustomUserDetails user) {

        // 7일 분석 서비스 호출
        AnalysisResponseDTO response = analysisService.getWeeklyAnalysis(user.userId());

        return ResponseEntity.ok(response);
    }
}
