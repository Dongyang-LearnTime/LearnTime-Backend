package learntime.backend.domain.study_member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study_member.dto.request.ChangeOwnerRequestDTO;
import learntime.backend.domain.study_member.dto.response.StudyMemberFriendResponseDTO;
import learntime.backend.domain.study_member.dto.response.StudyMemberResponseDTO;
import learntime.backend.domain.study_member.service.StudyInvitationService;
import learntime.backend.domain.study_member.service.StudyMemberService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study/member")
@RequiredArgsConstructor
@Tag(name = "공부 맴버 API", description = "공부 스터디 맴버 조회와 권한관리를 담당합니다. (JWT 필요)")
public class StudyMemberController {

    private final StudyMemberService studyMemberService;
    private final StudyInvitationService studyInvitationService;

    // 공부 맴버 조회
    @GetMapping("/{studyId}")
    @Operation(summary = "공부 스터디 맴버 목록 조회", description = "스터디 맴버 전체를 조회합니다.")
    public ResponseEntity<List<StudyMemberResponseDTO>> getStudyMemberList(@PathVariable Long studyId,
                                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudyMemberResponseDTO> result = studyMemberService.getAllStudyMember(studyId, userDetails.userId());
        return ResponseEntity.ok(result);
    }

    // 스터디 오너 넘거주기
    @PatchMapping("/owner")
    @Operation(summary = "공부 스터디 방장 권한 이양", description = "방장의 요청으로, 특정 맴버를 새로운 방장으로 바꿉니다.")
    public ResponseEntity<Void> changeStudyOwner(@Valid @RequestBody ChangeOwnerRequestDTO request,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyMemberService.changeStudyOwner(request, userDetails.userId());
        return ResponseEntity.ok().build();
    }

    // 스터디 초대용 친구 목록 조회
    @GetMapping("/{studyId}/friends")
    @Operation(summary = "스터디 초대용 친구 목록 조회", description = "친구 목록과 함께 스터디 가입/초대 상태를 함께 조회합니다.")
    public ResponseEntity<List<StudyMemberFriendResponseDTO>> getFriendsForStudyInvite(
            @PathVariable Long studyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudyMemberFriendResponseDTO> result =
                studyInvitationService.getFriendsForStudyInvite(studyId, userDetails.userId());
        return ResponseEntity.ok(result);
    }

}
