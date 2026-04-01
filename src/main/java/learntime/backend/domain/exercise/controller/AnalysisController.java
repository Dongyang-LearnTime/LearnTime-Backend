package learntime.backend.domain.exercise.controller;

import learntime.backend.domain.exercise.dto.response.AnalysisResponseDTO;
import learntime.backend.domain.exercise.service.AnalysisService;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercise/analysis") // 인증된 사용자 전용 경로
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    // 최근 7일 건강 데이터를 바탕으로 AI 분석 제안 조회
    @GetMapping("/weekly")
    public ResponseEntity<AnalysisResponseDTO> getWeeklyAnalysis(@AuthenticationPrincipal CustomUserDetails user) {

        // 7일 분석 서비스 호출
        AnalysisResponseDTO response = analysisService.getWeeklyAnalysis(user.userId());

        return ResponseEntity.ok(response);
    }
}
