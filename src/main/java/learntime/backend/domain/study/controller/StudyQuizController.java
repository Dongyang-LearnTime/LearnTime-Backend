package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.QuizCreateRequestDTO;
import learntime.backend.domain.study.dto.request.QuizSolveRequestDTO;
import learntime.backend.domain.study.dto.response.StudyQuizResponseDTO;
import learntime.backend.domain.study.dto.response.StudyQuizResultResponseDTO;
import learntime.backend.domain.study.service.facade.StudyQuizFacade;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/study/quiz")
@RequiredArgsConstructor
@Tag(name = "공부 퀴즈 API", description = "AI를 활용한 퀴즈 생성 및 풀이 완료 시 포인트 지급을 담당 (JWT 필요)")
public class StudyQuizController {

    private final StudyQuizFacade studyQuizFacade;

    @GetMapping("/{studyQuizId}")
    @Operation(
            summary = "퀴즈 조회",
            description = "생성된 퀴즈 정보와 퀴즈 문제를 조회함.")
    public ResponseEntity<StudyQuizResponseDTO> getStudyQuiz (@PathVariable Long studyQuizId) {
        StudyQuizResponseDTO result = studyQuizFacade.getStudyQuizDetail(studyQuizId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate")
    @Operation(
            summary = "필기 기반 퀴즈 생성",
            description = "필기 내용을 기반으로 퀴즈를 생성 후 DB에 저장하고 ID를 반환함.")
    public ResponseEntity<Long> createStudyQuiz(@Valid @RequestBody QuizCreateRequestDTO request,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long studyQuizId = studyQuizFacade.generateAndSaveStudyQuiz(request, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(studyQuizId);
    }

    @PostMapping("/solve")
    @Operation(
            summary = "퀴즈 풀이",
            description = "퀴즈 정답을 받아서 DB에 저장하고, 결과에 따라 포인트를 지급함.")
    public ResponseEntity<StudyQuizResultResponseDTO> solveStudyQuiz(@Valid @RequestBody List<QuizSolveRequestDTO> request,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyQuizResultResponseDTO result = studyQuizFacade.solveStudyQuiz(request, userDetails.userId());
        return ResponseEntity.ok(result);
    }

}
