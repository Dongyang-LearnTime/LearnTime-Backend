package learntime.backend.domain.study_progress.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study_progress.dto.request.PlanCompleteRequestDTO;
import learntime.backend.domain.study_progress.dto.request.FocusTimeRequestDTO;
import learntime.backend.domain.study_progress.dto.request.StudyUserContentRequestDTO;
import learntime.backend.domain.study_progress.dto.request.StudyUserContentUpdateRequestDTO;
import learntime.backend.domain.study_plan.dto.response.StudyDailyPlanInfoResponseDTO;
import learntime.backend.domain.study_plan.dto.response.StudyDailyPlanResponseDTO;
import learntime.backend.domain.study_progress.dto.response.StudyMemberContentResponseDTO;
import learntime.backend.domain.study_progress.service.StudyDailyService;
import java.util.List;
import learntime.backend.domain.study_progress.service.StudyUserContentService;
import learntime.backend.domain.study_progress.dto.response.TodayStudyPlanResponseDTO;
import learntime.backend.global.security.CustomUserDetails;
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
        StudyDailyPlanInfoResponseDTO result = studyDailyService.getStudyPlanInfoByDate(studyId, planDate, userDetails.userId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{studyId}/plan")
    @Operation(summary = "모든 일일 공부 진도 내용 조회", description = "특정 study의 모든 study_daily_plan 내용을 가져옵니다.")
    public ResponseEntity<List<StudyDailyPlanResponseDTO>> getStudyDailyPlansByStudyId(
            @PathVariable Long studyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudyDailyPlanResponseDTO> result = studyDailyService.findAllByStudyId(studyId, userDetails.userId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/today-plans")
    @Operation(summary = "오늘의 진도 목록 조회", description = "사용자가 가입한 모든 스터디 중 오늘 학습 계획이 있는 스터디의 계획 및 상태를 조회합니다.")
    public ResponseEntity<List<TodayStudyPlanResponseDTO>> getTodayPlans(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TodayStudyPlanResponseDTO> response = studyDailyService.getTodayPlans(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{studyDailyPlanId}/start")
    @Operation(summary = "일일 진도 시작", description = "공부 일일진도를 시작 상태로 변경합니다.")
    public ResponseEntity<Void> startPlan(@PathVariable Long studyDailyPlanId,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyDailyService.startStudyDailyPlan(studyDailyPlanId, userDetails.userId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/completion")
    @Operation(summary = "일일 진도 완료", description = "공부 일일진도를 완료로 변경하고 포인트를 지급합니다.")
    public ResponseEntity<String> completePlan(@Valid @RequestBody PlanCompleteRequestDTO request,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        int resultPoint = studyDailyService.completeStudyDailyPlan(request, userDetails.userId());
        return ResponseEntity.ok("포인트 " + resultPoint + "지급 완료!");
    }

    @PatchMapping("/focus-time")
    @Operation(summary = "일일 집중 시간 등록", description = "공부 일일 진도의 집중 시간을 등록합니다. (이미 완료/성공/실패된 계획인 경우 등록 불가능)")
    public ResponseEntity<Void> registerFocusTime(@Valid @RequestBody FocusTimeRequestDTO request,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyDailyService.registerFocusTime(request, userDetails.userId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/content")
    @Operation(summary = "일일 진도 내용 추가",
            description = "특정 일차의 공부 내용을 추가합니다. 저장 시 해당 계획의 상태가 '진행 중'으로 변경됩니다.")
    public ResponseEntity<Long> addUserContent(@Valid @RequestBody StudyUserContentRequestDTO request,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long studyUserContentId = studyUserContentService.addUserContent(request, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(studyUserContentId);
    }

    @PatchMapping("/content/{studyMemberContentId}")
    @Operation(summary = "일일 진도 내용 수정",
            description = "특정 일차의 공부 내용을 수정합니다.")
    public ResponseEntity<Void> updateUserContent(@PathVariable Long studyMemberContentId,
                                                  @Valid @RequestBody StudyUserContentUpdateRequestDTO request,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyUserContentService.updateUserContent(studyMemberContentId, request, userDetails.userId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{studyId}/content")
    @Operation(summary = "일일 진도 내용 조회", description = "planDate를 기준으로 특정 스터디의 일일 진도 내용과 사용자 공부 작성 내용 리스트를 조회합니다.")
    public ResponseEntity<StudyMemberContentResponseDTO> getUserContents(
            @PathVariable Long studyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudyMemberContentResponseDTO response = studyUserContentService.getUserContents(studyId, userDetails.userId(), planDate);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/content/{studyMemberContentId}")
    @Operation(summary = "일일 진도 내용 삭제", description = "특정 일차의 공부 작성 내용을 삭제합니다.")
    public ResponseEntity<Void> deleteUserContent(
            @PathVariable Long studyMemberContentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyUserContentService.deleteUserContent(studyMemberContentId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

}
