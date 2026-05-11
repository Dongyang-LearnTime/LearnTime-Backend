package learntime.backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.user.dto.request.UpdateNameRequestDTO;
import learntime.backend.domain.user.dto.request.UpdatePasswordRequestDTO;
import learntime.backend.domain.user.dto.response.MyPageResponseDTO;
import learntime.backend.domain.user.service.MyPageService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
@Tag(name = "마이페이지 API", description = "사용자 정보 조회 및 수정 관련 API")
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping
    @Operation(summary = "마이페이지 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<MyPageResponseDTO> getMyPage(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(myPageService.getMyInfo(user.getUsername()));
    }

    @PatchMapping("/name")
    @Operation(summary = "이름 수정", description = "사용자의 이름을 수정합니다.")
    public ResponseEntity<Void> updateName(@AuthenticationPrincipal CustomUserDetails user,
                                           @Valid @RequestBody UpdateNameRequestDTO request) {
        myPageService.updateName(user.getUsername(), request.name());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/password")
    @Operation(summary = "비밀번호 수정", description = "사용자의 비밀번호를 수정합니다.")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal CustomUserDetails user,
                                               @Valid @RequestBody UpdatePasswordRequestDTO request) {
        myPageService.updatePassword(user.getUsername(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }

}
