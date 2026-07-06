package learntime.backend.domain.user.service;

import learntime.backend.domain.user.dto.request.LoginRequestDTO;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.RefreshTokenRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.security.JwtProvider;
import learntime.backend.global.security.CustomUserDetails;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private User user;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .password("encoded_password")
                .name("tester")
                .role(Role.ROLE_USER)
                .build();

        loginRequest = new LoginRequestDTO("test@example.com", "password123");
    }

    @Test
    @DisplayName("로그인 성공 시 토큰이 정상적으로 발급되고 실패 카운트가 초기화된다.")
    void loginSuccess() {

        // given
        CustomUserDetails userDetails =
                new CustomUserDetails(
                        1L,
                        "test@example.com",
                        "tester",
                        "",
                        "ROLE_USER",
                        false
                );

        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        // 이메일 조회 mock
        given(userRepository.findByEmailForUpdate(loginRequest.email()))
                .willReturn(Optional.of(user));

        // authenticate 성공 mock
        given(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).willReturn(auth);

        // JWT 생성 mock
        given(jwtProvider.createToken(eq(user), any(Long.class)))
                .willReturn("refresh-token");
        given(jwtProvider.createAccessToken(eq(user), any(Long.class), any()))
                .willReturn("access-token");

        // when
        AuthService.TokenPair tokenPair =
                authService.login(loginRequest);

        // then
        assertThat(tokenPair).isNotNull();

        // 실패 횟수 초기화 확인
        assertThat(user.getFailedAttempts()).isEqualTo(0);

        // 계정 잠금 해제 상태 확인
        assertThat(user.isAccountLocked()).isFalse();

        // RefreshToken 저장 검증
        verify(refreshTokenRepository)
                .upsertToken(any(), any(), any());
    }

    @Test
    @DisplayName("비밀번호 실패 시 예외가 발생하고 실패 횟수가 1 증가한다.")
    void loginFailAndIncrementAttempts() {
        // given
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));
        given(userRepository.findByEmailForUpdate(loginRequest.email())).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.PASSWORD_NOT_MATCH.getMessage());

        assertThat(user.getFailedAttempts()).isEqualTo(1);
        assertThat(user.isAccountLocked()).isFalse();
    }

    @Test
    @DisplayName("비밀번호 5회 연속 실패 시 계정이 잠금 처리되고 LOCKED_ACCOUNT 예외가 발생한다.")
    void loginFailAndLockAccount() {
        // given
        user.incrementFailedAttempts();
        user.incrementFailedAttempts();
        user.incrementFailedAttempts();
        user.incrementFailedAttempts();
        // 현재 실패 횟수 4회 상태

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));
        given(userRepository.findByEmailForUpdate(loginRequest.email())).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.LOCKED_ACCOUNT.getMessage());

        assertThat(user.getFailedAttempts()).isEqualTo(5);
        assertThat(user.isAccountLocked()).isTrue();
    }
}
