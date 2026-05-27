package learntime.backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.user.dto.request.UpdateNameRequestDTO;
import learntime.backend.domain.user.dto.request.UpdatePasswordRequestDTO;
import learntime.backend.domain.user.dto.response.MyPageResponseDTO;
import learntime.backend.domain.user.service.MyPageService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import learntime.backend.domain.user.dto.response.TokenResponseDTO;
import learntime.backend.domain.user.service.AuthService;
import learntime.backend.global.utils.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
@Tag(name = "마이페이지 API", description = "사용자 정보 조회 및 수정 관련 API")
public class MyPageController {

    private final MyPageService myPageService;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "마이페이지 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<MyPageResponseDTO> getMyPage(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(myPageService.getMyInfo(user.getUsername()));
    }

    @PatchMapping("/name")
    @Operation(summary = "이름 수정", description = "사용자의 이름을 수정하고, 새로운 JWT 토큰을 쿠키와 반환값으로 줍니다.")
    public ResponseEntity<TokenResponseDTO> updateName(@AuthenticationPrincipal CustomUserDetails user,
                                            @Valid @RequestBody UpdateNameRequestDTO request) {
        AuthService.TokenPair token = myPageService.updateName(user.getUsername(), request.name());

        ResponseCookie cookie = CookieUtil.createRefreshTokenCookie(token.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponseDTO(token.accessToken(), "Bearer"));
    }

    @PatchMapping("/password")
    @Operation(summary = "비밀번호 수정", description = "사용자의 비밀번호를 수정하고 로그아웃 처리합니다.")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal CustomUserDetails user,
                                               @CookieValue(value = "refreshToken", required = false) String refreshToken,
                                               HttpServletResponse response,
                                               @Valid @RequestBody UpdatePasswordRequestDTO request) {
        myPageService.updatePassword(user.getUsername(), request.currentPassword(), request.newPassword());
        
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie cookie = CookieUtil.createEmptyRefreshTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok().build();
    }

}
