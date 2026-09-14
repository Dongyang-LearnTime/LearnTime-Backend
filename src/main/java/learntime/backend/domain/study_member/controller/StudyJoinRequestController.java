package learntime.backend.domain.study_member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.study_member.dto.response.StudyJoinRequestResponseDTO;
import learntime.backend.domain.study_member.service.StudyJoinRequestService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "스터디 가입 요청 API", description = "스터디 가입 요청 및 방장 승인/거절을 관리합니다. (JWT 필요)")
@RestController
@RequestMapping("/api/study/member/join-request")
@RequiredArgsConstructor
public class StudyJoinRequestController {

    private final StudyJoinRequestService studyJoinRequestService;

    @Operation(summary = "공개 스터디 가입 요청", description = "일반 사용자가 공개 스터디에 가입을 요청합니다.")
    @PostMapping("/{studyId}")
    public ResponseEntity<Long> requestJoin(
            @PathVariable Long studyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long requestId = studyJoinRequestService.requestJoin(studyId, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(requestId);
    }

    @Operation(summary = "스터디 가입 요청 승인", description = "스터디 방장이 지원자의 가입 요청을 승인하고 스터디 멤버로 등록합니다.")
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<Long> approveRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long studyMemberId = studyJoinRequestService.approveRequest(requestId, userDetails.userId());
        return ResponseEntity.ok(studyMemberId);
    }

    @Operation(summary = "스터디 가입 요청 거절", description = "스터디 방장이 지원자의 가입 요청을 거절합니다.")
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<Void> rejectRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        studyJoinRequestService.rejectRequest(requestId, userDetails.userId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "스터디 가입 요청 취소", description = "요청자 본인이 대기 중인 가입 요청을 취소합니다.")
    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<Void> cancelRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        studyJoinRequestService.cancelRequest(requestId, userDetails.userId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "스터디 대기 가입 요청 목록 조회 (방장 전용)", description = "방장이 본인 스터디에 들어온 대기(PENDING) 가입 요청 목록을 조회합니다.")
    @GetMapping("/{studyId}/pending")
    public ResponseEntity<List<StudyJoinRequestResponseDTO>> getPendingRequests(
            @PathVariable Long studyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<StudyJoinRequestResponseDTO> response = studyJoinRequestService.getPendingRequestsForStudy(studyId, userDetails.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 가입 요청 내역 목록 조회", description = "사용자가 자신이 신청한 스터디 가입 요청 목록과 상태를 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<List<StudyJoinRequestResponseDTO>> getMyJoinRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<StudyJoinRequestResponseDTO> response = studyJoinRequestService.getMyJoinRequests(userDetails.userId());
        return ResponseEntity.ok(response);
    }
}
