package learntime.backend.domain.user.service;

import learntime.backend.domain.user.dto.request.EmailVerificationConfirmRequestDTO;
import learntime.backend.domain.user.dto.request.EmailVerificationRequestDTO;
import learntime.backend.domain.user.dto.response.EmailVerificationResponseDTO;
import learntime.backend.domain.user.model.EmailVerification;
import learntime.backend.domain.user.repository.EmailVerificationRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.code.EmailErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.exception.EmailException;
import learntime.backend.global.utils.EmailUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String PEPPER = "test-pepper";
    private static final String EMAIL = "test@example.com";

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("인증 코드 발송 시 코드를 해시로 저장하고 인증 메일 이벤트를 발행한다.")
    void sendVerificationCode() {
        ReflectionTestUtils.setField(emailVerificationService, "pepper", PEPPER);
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(emailVerificationRepository.save(any(EmailVerification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.sendVerificationCode(new EmailVerificationRequestDTO(EMAIL));

        ArgumentCaptor<EmailVerification> verificationCaptor =
                ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository).save(verificationCaptor.capture());

        EmailVerification savedVerification = verificationCaptor.getValue();
        assertThat(savedVerification.getEmail()).isEqualTo(EMAIL);
        assertThat(savedVerification.getCodeHash()).isNotBlank();
        assertThat(savedVerification.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(4));

        verify(eventPublisher).publishEvent(any(learntime.backend.domain.user.event.EmailSendEvent.class));
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 인증 코드를 발송하지 않는다.")
    void sendVerificationCodeDuplicatedEmail() {
        ReflectionTestUtils.setField(emailVerificationService, "pepper", PEPPER);
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(new EmailVerificationRequestDTO(EMAIL)))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.USER_EMAIL_DUPLICATED.getMessage());

        verifyNoInteractions(emailVerificationRepository, eventPublisher);
    }

    @Test
    @DisplayName("인증 코드가 일치하면 회원가입용 일회성 토큰을 발급한다.")
    void verifyCodeSuccess() {
        ReflectionTestUtils.setField(emailVerificationService, "pepper", PEPPER);
        EmailVerification verification = EmailVerification.create(
                EMAIL,
                hash(EMAIL, "123456"),
                LocalDateTime.now().plusMinutes(5)
        );
        given(emailVerificationRepository.findTopByEmailOrderByEmailVerificationIdDesc(EMAIL))
                .willReturn(Optional.of(verification));

        EmailVerificationResponseDTO response =
                emailVerificationService.verifyCode(new EmailVerificationConfirmRequestDTO(EMAIL, "123456"));

        assertThat(response.verificationToken()).isNotBlank();
        assertThat(verification.isVerified()).isTrue();
        assertThat(verification.getVerificationTokenHash()).isNotBlank();
    }

    @Test
    @DisplayName("인증 코드가 일치하지 않으면 시도 횟수를 증가시키고 예외를 던진다.")
    void verifyCodeMismatch() {
        ReflectionTestUtils.setField(emailVerificationService, "pepper", PEPPER);
        EmailVerification verification = EmailVerification.create(
                EMAIL,
                hash(EMAIL, "123456"),
                LocalDateTime.now().plusMinutes(5)
        );
        given(emailVerificationRepository.findTopByEmailOrderByEmailVerificationIdDesc(EMAIL))
                .willReturn(Optional.of(verification));

        assertThatThrownBy(() ->
                emailVerificationService.verifyCode(new EmailVerificationConfirmRequestDTO(EMAIL, "000000")))
                .isInstanceOf(EmailException.class)
                .hasMessage(EmailErrorCode.EMAIL_CODE_MISMATCH.getMessage());

        assertThat(verification.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("만료된 인증 코드는 검증할 수 없다.")
    void verifyCodeExpired() {
        ReflectionTestUtils.setField(emailVerificationService, "pepper", PEPPER);
        EmailVerification verification = EmailVerification.create(
                EMAIL,
                hash(EMAIL, "123456"),
                LocalDateTime.now().minusSeconds(1)
        );
        given(emailVerificationRepository.findTopByEmailOrderByEmailVerificationIdDesc(EMAIL))
                .willReturn(Optional.of(verification));

        assertThatThrownBy(() ->
                emailVerificationService.verifyCode(new EmailVerificationConfirmRequestDTO(EMAIL, "123456")))
                .isInstanceOf(EmailException.class)
                .hasMessage(EmailErrorCode.EMAIL_CODE_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("회원가입 인증 토큰이 유효하면 소비 처리한다.")
    void verifySignupTokenSuccess() {
        ReflectionTestUtils.setField(emailVerificationService, "pepper", PEPPER);
        String rawToken = "signup-token";
        String tokenHash = hash(EMAIL, rawToken);
        EmailVerification verification = EmailVerification.create(
                EMAIL,
                hash(EMAIL, "123456"),
                LocalDateTime.now().plusMinutes(5)
        );
        verification.markVerified(tokenHash);

        given(emailVerificationRepository
                .findTopByEmailAndVerificationTokenHashAndConsumedAtIsNullOrderByEmailVerificationIdDesc(EMAIL, tokenHash))
                .willReturn(Optional.of(verification));

        emailVerificationService.verifySignupToken(EMAIL, rawToken);

        assertThat(verification.isConsumed()).isTrue();
    }

    private static String hash(String email, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((email + ":" + value + ":" + PEPPER).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
