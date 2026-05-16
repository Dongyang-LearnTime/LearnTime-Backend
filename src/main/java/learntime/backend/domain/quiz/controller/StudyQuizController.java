package learntime.backend.domain.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.quiz.dto.request.QuizCreateRequestDTO;
import learntime.backend.domain.quiz.dto.request.QuizSolveRequestDTO;
import learntime.backend.domain.quiz.dto.request.UpdateQuizTitleRequestDTO;
import learntime.backend.domain.quiz.dto.response.QuizHistoryListResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizListResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizResultResponseDTO;
import learntime.backend.domain.quiz.service.StudyQuizFacade;
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
            description = "생성된 퀴즈 정보와 퀴즈 문항을 조회함.")
    public ResponseEntity<StudyQuizResponseDTO> getStudyQuiz (@PathVariable Long studyQuizId,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyQuizResponseDTO result = studyQuizFacade.getStudyQuizWithQuestions(studyQuizId, userDetails.userId());
        return ResponseEntity.ok(result);
    }


    @GetMapping("/list/{studyMemberId}")
    @Operation(summary = "퀴즈 목록 조회", description = "특정 스터디의 퀴즈 목록을 조회함.")
    public ResponseEntity<StudyQuizListResponseDTO> getStudyQuizList(@PathVariable Long studyMemberId,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyQuizListResponseDTO result = studyQuizFacade.getStudyQuizList(studyMemberId, userDetails.userId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{studyQuizId}/history/list")
    @Operation(summary = "퀴즈 풀이 이력 목록 조회", description = "특정 퀴즈의 풀이 이력 목록을 조회함.")
    public ResponseEntity<QuizHistoryListResponseDTO> getQuizHistoryList(@PathVariable Long studyQuizId,
                                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        QuizHistoryListResponseDTO result = studyQuizFacade.getQuizHistoryList(studyQuizId, userDetails.userId());
        return ResponseEntity.ok(result);
    }


    @GetMapping("/history/{quizHistoryId}/result")
    @Operation(
            summary = "퀴즈 풀이 결과 조회",
            description = "퀴즈 문제, 사용자가 제출한 답안, 실제 정답 및 정답 여부 등을 조회함."
    )
    public ResponseEntity<StudyQuizResultResponseDTO> getStudyQuizResult(@PathVariable Long quizHistoryId,
                                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyQuizResultResponseDTO result = studyQuizFacade.getQuizResult(quizHistoryId, userDetails.userId());
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
    public ResponseEntity<Long> solveStudyQuiz(@Valid @RequestBody List<QuizSolveRequestDTO> request,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long quizHistoryId = studyQuizFacade.solveStudyQuiz(request, userDetails.userId());
        return ResponseEntity.ok(quizHistoryId);
    }


    @PatchMapping("/title")
    @Operation(summary = "퀴즈 제목 변경", description = "퀴즈 제목 변경 후 DB에 저장함.")
    public ResponseEntity<Void> changeTitle(
            @Valid @RequestBody UpdateQuizTitleRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        studyQuizFacade.updateTitle(request, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{studyQuizId}")
    @Operation(summary = "퀴즈 삭제", description = "공부 퀴즈와 하위 테이블을 DB에서 삭제함.")
    public ResponseEntity<Void> deleteStudyQuiz(@PathVariable Long studyQuizId,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyQuizFacade.deleteStudyQuiz(studyQuizId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/history/{quizHistoryId}")
    @Operation(summary = "퀴즈 이력 삭제", description = "퀴즈 풀이 이력, 사용자 답안을 DB에서 삭제함.")
    public ResponseEntity<Void> deleteQuizHistory(@PathVariable Long quizHistoryId,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyQuizFacade.deleteQuizHistory(quizHistoryId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }


}
