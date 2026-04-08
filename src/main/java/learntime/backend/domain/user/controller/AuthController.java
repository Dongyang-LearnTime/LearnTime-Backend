package learntime.backend.domain.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import learntime.backend.domain.user.dto.request.LoginRequestDTO;
import learntime.backend.domain.user.dto.request.SignUpRequestDTO;
import learntime.backend.domain.user.dto.response.TokenResponseDTO;
import learntime.backend.domain.user.service.AuthService;
import learntime.backend.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    private static final long REFRESH_TIME = 60 * 60 * 24 * 14; // 초 단위, 14일

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(
            @RequestBody LoginRequestDTO request,
            HttpServletResponse response
    ) {
        AuthService.TokenPair token = authService.login(request);

        // 리프레쉬 토큰 HttpOnly 쿠키로 전달
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(REFRESH_TIME)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(
                new TokenResponseDTO(token.accessToken(), "Bearer")
        );
    }


    // AccessToken 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDTO> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AuthService.TokenPair tokenPair = authService.refresh(refreshToken);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenPair.refreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(REFRESH_TIME)
                    .sameSite("None")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new TokenResponseDTO(tokenPair.accessToken(), "Bearer"));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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

        // 쿠키 삭제(덮어씌우기)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok("logout success");
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signupUser(@Valid @RequestBody SignUpRequestDTO request) {
        authService.createUser(request.userName(), request.email(), request.password());
        log.info("{} 회원가입 성공!", request.userName());
        return ResponseEntity.ok().build();
    }

}