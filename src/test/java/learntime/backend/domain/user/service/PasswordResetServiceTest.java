package learntime.backend.domain.user.service;

import learntime.backend.domain.user.dto.request.PasswordResetConfirmRequestDTO;
import learntime.backend.domain.user.dto.request.PasswordResetSendRequestDTO;
import learntime.backend.domain.user.dto.request.PasswordResetVerifyRequestDTO;
import learntime.backend.domain.user.dto.response.PasswordResetVerifyResponseDTO;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.event.PasswordResetEmailSendEvent;
import learntime.backend.domain.user.model.EmailVerification;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.EmailVerificationRepository;
import learntime.backend.domain.user.repository.RefreshTokenRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.code.EmailErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.exception.EmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String PEPPER = "test-pepper";
    private static final String EMAIL = "test@example.com";

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "pepper", PEPPER);
        ReflectionTestUtils.setField(passwordResetService, "authCodeExpiresMinutes", 10);
    }

    @Test
    @DisplayName("가입된 유저가 이메일 재설정 요청 시 6자리 코드 저장 및 이벤트를 발행한다.")
    void sendPasswordResetCode_success() {
        // given
        User user = User.builder()
                .email(EMAIL)
                .name("테스터")
                .socialProvider(AuthProvider.LOCAL)
                .role(Role.ROLE_USER)
                .build();

        given(emailVerificationRepository.countByEmailAndCreatedAtAfter(any(), any())).willReturn(0);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));

        // when
        passwordResetService.sendPasswordResetCode(new PasswordResetSendRequestDTO(EMAIL));

        // then
        verify(emailVerificationRepository).save(any(EmailVerification.class));
        verify(eventPublisher).publishEvent(any(PasswordResetEmailSendEvent.class));
    }

    @Test
    @DisplayName("소셜 로그인 전용 계정으로 재설정 요청 시 예외가 발생한다.")
    void sendPasswordResetCode_socialUser_throwsException() {
        // given
        User socialUser = User.builder()
                .email(EMAIL)
                .name("소셜유저")
                .socialId("google-123")
                .socialProvider(AuthProvider.GOOGLE)
                .role(Role.ROLE_USER)
                .build();

        given(emailVerificationRepository.countByEmailAndCreatedAtAfter(any(), any())).willReturn(0);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(socialUser));

        // when & then
        assertThatThrownBy(() -> passwordResetService.sendPasswordResetCode(new PasswordResetSendRequestDTO(EMAIL)))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.SOCIAL_USER_CANNOT_RESET_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("올바른 6자리 코드 검증 시 Reset Token을 반환한다.")
    void verifyPasswordResetCode_success() {
        // given
        String code = "123456";
        String codeHash = hash(EMAIL, code);
        EmailVerification verification = EmailVerification.create(EMAIL, codeHash, LocalDateTime.now().plusMinutes(10));

        given(emailVerificationRepository.findTopByEmailOrderByEmailVerificationIdDesc(EMAIL))
                .willReturn(Optional.of(verification));

        // when
        PasswordResetVerifyResponseDTO response = passwordResetService.verifyPasswordResetCode(
                new PasswordResetVerifyRequestDTO(EMAIL, code)
        );

        // then
        assertThat(response.resetToken()).isNotBlank();
        assertThat(verification.isVerified()).isTrue();
    }

    @Test
    @DisplayName("Reset Token 제출 시 비밀번호가 변경되고 기존 토큰이 삭제되며 Reset Token이 소모된다.")
    void confirmPasswordReset_success() {
        // given
        String resetToken = "valid-reset-token";
        String tokenHash = hash(EMAIL, resetToken);
        EmailVerification verification = EmailVerification.create(EMAIL, "code-hash", LocalDateTime.now().plusMinutes(10));
        verification.markVerified(tokenHash);

        User user = User.builder()
                .email(EMAIL)
                .name("테스터")
                .password("oldEncryptedPassword")
                .socialProvider(AuthProvider.LOCAL)
                .build();

        given(emailVerificationRepository.findTopByEmailAndVerificationTokenHashAndConsumedAtIsNullOrderByEmailVerificationIdDesc(EMAIL, tokenHash))
                .willReturn(Optional.of(verification));
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("NewPassword123!@")).willReturn("newEncryptedPassword");

        // when
        passwordResetService.confirmPasswordReset(new PasswordResetConfirmRequestDTO(EMAIL, resetToken, "NewPassword123!@"));

        // then
        assertThat(user.getPassword()).isEqualTo("newEncryptedPassword");
        assertThat(verification.isConsumed()).isTrue();
        verify(refreshTokenRepository).deleteByUser(user);
    }

    private String hash(String email, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((email + ":" + value + ":" + PEPPER).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
