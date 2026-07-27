package learntime.backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.user.dto.request.PasswordResetConfirmRequestDTO;
import learntime.backend.domain.user.dto.request.PasswordResetSendRequestDTO;
import learntime.backend.domain.user.dto.request.PasswordResetVerifyRequestDTO;
import learntime.backend.domain.user.dto.response.PasswordResetVerifyResponseDTO;
import learntime.backend.domain.user.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
@Tag(name = "비밀번호 재설정 API", description = "로그인하지 않은 사용자의 비밀번호 재설정을 위한 이메일 인증 API (JWT 필요 없음)")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    @Operation(summary = "비밀번호 재설정 이메일 인증 코드 발송", description = "가입된 이메일로 비밀번호 재설정용 6자리 인증 코드를 발송합니다.")
    public ResponseEntity<Void> sendPasswordResetCode(@Valid @RequestBody PasswordResetSendRequestDTO request) {
        passwordResetService.sendPasswordResetCode(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    @Operation(summary = "비밀번호 재설정 인증 코드 확인", description = "6자리 인증 코드를 확인하고 비밀번호 변경에 사용될 일회성 Reset Token을 발급합니다.")
    public ResponseEntity<PasswordResetVerifyResponseDTO> verifyPasswordResetCode(
            @Valid @RequestBody PasswordResetVerifyRequestDTO request
    ) {
        return ResponseEntity.ok(passwordResetService.verifyPasswordResetCode(request));
    }

    @PostMapping("/confirm")
    @Operation(summary = "비밀번호 최종 재설정", description = "발급받은 Reset Token과 새 비밀번호를 제출하여 비밀번호를 최종 변경합니다.")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequestDTO request) {
        passwordResetService.confirmPasswordReset(request);
        return ResponseEntity.ok().build();
    }
}
