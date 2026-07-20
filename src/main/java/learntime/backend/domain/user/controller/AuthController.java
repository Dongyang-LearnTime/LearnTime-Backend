package learntime.backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import learntime.backend.domain.user.dto.request.EmailVerificationConfirmRequestDTO;
import learntime.backend.domain.user.dto.request.EmailVerificationRequestDTO;
import learntime.backend.domain.user.dto.request.LoginRequestDTO;
import learntime.backend.domain.user.dto.request.SignUpRequestDTO;
import learntime.backend.domain.user.dto.request.SocialLoginRequestDTO;
import learntime.backend.domain.user.dto.request.SocialSignUpRequestDTO;
import learntime.backend.domain.user.dto.response.EmailVerificationResponseDTO;
import learntime.backend.domain.user.dto.response.SocialLoginResponseDTO;
import learntime.backend.domain.user.dto.response.TokenResponseDTO;
import learntime.backend.domain.user.service.AuthService;
import learntime.backend.domain.user.service.OAuth2Service;
import learntime.backend.domain.user.service.EmailVerificationService;
import learntime.backend.domain.user.service.UserService;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.utils.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "사용자 인증 API", description = "로그인, 로그아웃, 회원가입 등을 관리하는 인증 API (JWT 필요 없음)")
public class AuthController {

    private final AuthService authService;
    private final OAuth2Service oAuth2Service;
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    @Value("${jwt.refresh-expiration}")
    private long refreshTime; // 리프레쉬 토큰 유효기간

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일, 비밀번호를 받아 검증 후 JWT 토큰을 쿠키와 반환값으로 줌.")
    public ResponseEntity<TokenResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletResponse response
    ) {
        AuthService.TokenPair token = authService.login(request);

        // 리프레쉬 토큰 HttpOnly 쿠키로 전달
        ResponseCookie cookie =
                CookieUtil.createRefreshTokenCookie(token.refreshToken(), refreshTime);

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(
                new TokenResponseDTO(token.accessToken(), "Bearer")
        );
    }

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


    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "DB의 리프레쉬 토큰을 확인하여 토큰을 재발급함.")
    public ResponseEntity<TokenResponseDTO> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AuthService.TokenPair tokenPair = authService.refresh(refreshToken);

            ResponseCookie cookie =
                    CookieUtil.createRefreshTokenCookie(tokenPair.refreshToken(), refreshTime);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new TokenResponseDTO(tokenPair.accessToken(), "Bearer"));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "DB에 리프레쉬 토큰 및 쿠키를 삭제함.")
    public ResponseEntity<?> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // 쿠키 삭제(덮어씌우기)
        ResponseCookie cookie = CookieUtil.createEmptyRefreshTokenCookie();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok("logout success");
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "계정을 생성함.")
    public ResponseEntity<String> signupUser(@Valid @RequestBody SignUpRequestDTO request) {
        authService.createUser(request);
        log.info("{} 회원가입 성공!", request.userName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email-verifications")
    @Operation(summary = "회원가입 이메일 인증 코드 발송", description = "회원가입 전 이메일로 6자리 인증 코드를 발송합니다.")
    public ResponseEntity<Void> sendEmailVerificationCode(@Valid @RequestBody EmailVerificationRequestDTO request) {
        emailVerificationService.sendVerificationCode(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email-verifications/verify")
    @Operation(summary = "회원가입 이메일 인증 코드 확인", description = "인증 코드를 확인하고 회원가입에 사용할 일회성 토큰을 발급합니다.")
    public ResponseEntity<EmailVerificationResponseDTO> verifyEmailVerificationCode(
            @Valid @RequestBody EmailVerificationConfirmRequestDTO request
    ) {
        return ResponseEntity.ok(emailVerificationService.verifyCode(request));
    }

    // 이름 중복 체크
    @GetMapping("/name/{name}")
    @Operation(summary = "이름 중복 체크", description = "사용 가능한 이름인지 중복 체크합니다.")
    public ResponseEntity<Boolean> checkName(@PathVariable String name) {
        // true면 사용 가능, false면 이미 있음
        return ResponseEntity.ok(!userService.isNameDuplicated(name));
    }

    // 이메일 중복 체크
    @GetMapping("/email/{email}")
    @Operation(summary = "이메일 중복 체크", description = "사용 가능한 이메일인지 중복 체크합니다.")
    public ResponseEntity<Boolean> checkEmail(@PathVariable String email) {
        // true면 사용 가능, false면 이미 있음
        return ResponseEntity.ok(!userService.isEmailDuplicated(email));
    }

}
