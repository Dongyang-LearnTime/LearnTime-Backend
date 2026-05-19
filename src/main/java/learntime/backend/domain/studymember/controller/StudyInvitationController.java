package learntime.backend.domain.studymember.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.studymember.dto.request.StudyMemberRequestDTO;
import learntime.backend.domain.studymember.dto.response.StudyInvitationResponseDTO;
import learntime.backend.domain.studymember.service.StudyInvitationService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study/member/request")
@RequiredArgsConstructor
@Tag(name = "공부 초대 요청 API", description = "공부 스터디 맴버의 초대, 수락 등을 담당합니다. (JWT 필요)")
public class StudyInvitationController {

    private final StudyInvitationService studyInvitationService;

    @GetMapping("/invited")
    @Operation(summary = "받은 초대 목록 조회", description = "초대 받은 사람의 userId를 기준으로 초대 요청 목록을 조회합니다.")
    public ResponseEntity<List<StudyInvitationResponseDTO>> getReceivedInvitations(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudyInvitationResponseDTO> result = studyInvitationService.getReceivedInvitationList(userDetails.userId());
        return ResponseEntity.ok(result);
    }


    @GetMapping("/inviter")
    @Operation(summary = "보낸 초대 목록 조회", description = "초대 보낸 사람의 userId를 기준으로 초대 목록을 조회합니다.")
    public ResponseEntity<List<StudyInvitationResponseDTO>> getSendInvitations(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudyInvitationResponseDTO> result = studyInvitationService.getSentInvitationList(userDetails.userId());
        return ResponseEntity.ok(result);
    }


    @PostMapping
    @Operation(summary = "공부 스터디 맴버 초대", description = "공부 스터디의 방장이 다른 사용자를 초대합니다.")
    public ResponseEntity<Long> inviteMember(@Valid @RequestBody StudyMemberRequestDTO request,
                                             @AuthenticationPrincipal CustomUserDetails userDetails)  {

        Long studyRequestId = studyInvitationService.inviteMember(request, userDetails.userId());
        return ResponseEntity.ok(studyRequestId);
    }

    @PatchMapping("/{invitationId}/accept")
    @Operation(summary = "공부 스터디 초대 승인", description = "초대를 받은 사용자가 초대를 승인합니다.")
    public ResponseEntity<Void> approveRequest(@PathVariable Long invitationId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyInvitationService.approveRequest(invitationId, userDetails.userId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{invitationId}/reject")
    @Operation(summary = "공부 스터디 초대 거절", description = "초대를 받은 사용자가 초대를 거절합니다.")
    public ResponseEntity<Void> rejectRequest(@PathVariable Long invitationId,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyInvitationService.rejectRequest(invitationId, userDetails.userId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{invitationId}")
    @Operation(summary = "공부 스터디 초대 취소", description = "초대를 보낸 사용자가 초대를 취소합니다.")
    public ResponseEntity<Void> cancelRequest(@PathVariable Long invitationId,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyInvitationService.cancelRequest(invitationId, userDetails.userId());
        return ResponseEntity.ok().build();
    }

}
