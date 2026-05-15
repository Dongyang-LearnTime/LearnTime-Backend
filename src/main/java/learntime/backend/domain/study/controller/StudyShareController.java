package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.study.dto.response.SharedStudyResponseDTO;
import learntime.backend.domain.study.dto.response.StudyParticipantResponseDTO;
import learntime.backend.domain.study.service.core.StudyShareService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study/share")
@RequiredArgsConstructor
@Tag(name = "공부 일정 공유 API", description = "친구와 공유한 공부 일정 참가자 조회 및 나가기 API (JWT 필요)")
public class StudyShareController {

    private final StudyShareService studyShareService;

    @GetMapping("/my")
    @Operation(summary = "내 공유 공부 일정 조회", description = "현재 로그인한 사용자가 참여 중인 공유 공부 일정을 조회합니다.")
    public ResponseEntity<List<SharedStudyResponseDTO>> getMySharedStudies(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(studyShareService.getMySharedStudies(userDetails.userId()));
    }

    @GetMapping("/{studyId}/participants")
    @Operation(summary = "공유 공부 일정 참가자 조회", description = "공유 공부 일정의 현재 참가자 목록을 조회합니다.")
    public ResponseEntity<List<StudyParticipantResponseDTO>> getParticipants(
            @PathVariable Long studyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(studyShareService.getParticipants(studyId, userDetails.userId()));
    }

    @DeleteMapping("/{studyId}/me")
    @Operation(summary = "공유 공부 일정 나가기", description = "친구가 공유한 공부 일정에서 나갑니다. 생성자는 나갈 수 없습니다.")
    public ResponseEntity<Void> leaveStudy(
            @PathVariable Long studyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyShareService.leaveStudy(studyId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }
}
