package learntime.backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import learntime.backend.domain.user.dto.request.SocialLoginRequestDTO;
import learntime.backend.domain.user.dto.request.SocialSignUpRequestDTO;
import learntime.backend.domain.user.dto.response.SocialLoginResponseDTO;
import learntime.backend.domain.user.dto.response.TokenResponseDTO;
import learntime.backend.domain.user.service.AuthService;
import learntime.backend.domain.user.service.OAuth2Service;
import learntime.backend.global.utils.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "소셜 인증 API", description = "소셜 로그인 및 소셜 회원가입을 관리하는 API (JWT 필요 없음)")
public class SocialAuthController {

    private final OAuth2Service oAuth2Service;

    @Value("${jwt.refresh-expiration}")
    private long refreshTime;

    @PostMapping("/social/login")
    @Operation(summary = "소셜 로그인", description = "소셜 제공자 토큰을 받아 검증 후 가입 여부를 반환합니다. 가입되어 있다면 토큰을 반환합니다.")
    public ResponseEntity<SocialLoginResponseDTO> socialLogin(
            @Valid @RequestBody SocialLoginRequestDTO request,
            HttpServletResponse response
    ) {
        Optional<AuthService.TokenPair> tokenOpt = oAuth2Service.socialLogin(request);

        if (tokenOpt.isEmpty()) {
            // 미가입 유저
            return ResponseEntity.ok(SocialLoginResponseDTO.notRegistered());
        }

        // 가입된 유저
        AuthService.TokenPair token = tokenOpt.get();
        ResponseCookie cookie = CookieUtil.createRefreshTokenCookie(token.refreshToken(), refreshTime);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(SocialLoginResponseDTO.success(token.accessToken()));
    }

    @PostMapping("/social/signup")
    @Operation(summary = "소셜 회원가입", description = "미가입 소셜 유저가 닉네임과 약관 동의를 완료하여 가입을 요청합니다. 완료 시 로그인 토큰을 반환합니다.")
    public ResponseEntity<TokenResponseDTO> socialSignUp(
            @Valid @RequestBody SocialSignUpRequestDTO request,
            HttpServletResponse response
    ) {
        AuthService.TokenPair token = oAuth2Service.socialSignUp(request);

        ResponseCookie cookie = CookieUtil.createRefreshTokenCookie(token.refreshToken(), refreshTime);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new TokenResponseDTO(token.accessToken(), "Bearer"));
    }
}
