package learntime.backend.domain.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import learntime.backend.domain.user.dto.request.LoginRequest;
import learntime.backend.domain.user.dto.response.TokenResponse;
import learntime.backend.domain.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private static final long REFRESH_TIME = 60 * 60 * 24 * 14; // 초 단위, 14일

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthService.TokenPair token = authService.login(request);

        // 리프레쉬 토큰 HttpOnly 쿠키로 전달
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(REFRESH_TIME)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(
                new TokenResponse(token.accessToken(), "Bearer")
        );
    }


    // AccessToken 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response
    ) {
        AuthService.TokenPair token = authService.refresh(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", token.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(REFRESH_TIME)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(
                new TokenResponse(token.accessToken(), "Bearer")
        );
    }


    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // 쿠키 삭제(덮어쒸우기)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok("logout success");
    }
}