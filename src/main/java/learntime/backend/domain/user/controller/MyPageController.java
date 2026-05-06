package learntime.backend.domain.user.controller;

import jakarta.validation.Valid;
import learntime.backend.domain.user.dto.request.SignUpRequestDTO;
import learntime.backend.domain.user.dto.response.MyPageResponseDTO;
import learntime.backend.domain.user.service.UserService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;

     // 마이페이지 정보 조회
    @GetMapping("/me")
    public ResponseEntity<MyPageResponseDTO> getMyPage(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getMyInfo(user.getUsername()));
    }

    // 마이페이지 정보 수정
    @PutMapping("/me")
    public ResponseEntity<MyPageResponseDTO> updateMyPage(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody SignUpRequestDTO request) { // 기존 DTO 재사용

        return ResponseEntity.ok(userService.updateMyInfo(user.getUsername(), request));
    }
}
