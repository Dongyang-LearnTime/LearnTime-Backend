package learntime.backend.domain.studymember.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.studymember.dto.request.StudyMemberRequestDTO;
import learntime.backend.domain.studymember.service.StudyInvitationService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study/member/request")
@RequiredArgsConstructor
@Tag(name = "공부 사용자 요청", description = "공부 스터디 맴버의 초대, 수락 등을 담당합니다.")
public class StudyInvitationController {

    private final StudyInvitationService studyInvitationService;

    //
//    getReceivedRequests()  // 받은 초대 목록 조회
//
//    getSentRequests()      // 보낸 초대 목록 조회
//
//    getPendingRequests()   // 대기 중 초대 목록 조회

    @PostMapping
    @Operation(summary = "공부 스터디 맴버 초대", description = "공부 맴버의 Owner가 다른 사용자를 초대합니다.")
    public ResponseEntity<Long> inviteMember(@Valid @RequestBody StudyMemberRequestDTO request,
                                             @AuthenticationPrincipal CustomUserDetails userDetails)  {

        Long studyRequestId = studyInvitationService.inviteMember(request, userDetails.userId());
        return ResponseEntity.ok(studyRequestId);
    }

//    approveRequest()       // 초대 승인
//
//    rejectRequest()        // 초대 거절
//
//    cancelRequest()        // 초대 취소

}
