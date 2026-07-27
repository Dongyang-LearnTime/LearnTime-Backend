package learntime.backend.domain.user.service;

import learntime.backend.domain.user.dto.request.PasswordResetConfirmRequestDTO;
import learntime.backend.domain.user.dto.request.PasswordResetSendRequestDTO;
import learntime.backend.domain.user.dto.request.PasswordResetVerifyRequestDTO;
import learntime.backend.domain.user.dto.response.PasswordResetVerifyResponseDTO;
import learntime.backend.domain.user.enums.AuthProvider;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int AUTH_CODE_BOUND = 1_000_000;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.pepper}")
    private String pepper;

    @Value("${mail.verification-expiration-minutes:10}")
    private int authCodeExpiresMinutes;

    // 1. 비밀번호 재설정 이메일 인증 코드 발송
    @Transactional
    public void sendPasswordResetCode(PasswordResetSendRequestDTO request) {
        String email = normalizeEmail(request.email());

        // 1분 이내 동일 이메일 요청 비율 제한 검증 (최대 3회)
        checkRateLimit(email);

        // 가입 유저 확인 및 소셜 전용 계정 여부 검증
        getLocalUserByEmail(email);

        // 6자리 인증 코드 생성 및 해싱
        String code = generateAuthCode();
        String codeHash = hash(email, code);

        // 인증 정보 DB 저장
        EmailVerification verification = EmailVerification.create(
                email,
                codeHash,
                LocalDateTime.now().plusMinutes(authCodeExpiresMinutes)
        );
        emailVerificationRepository.save(verification);

        // 비동기 비밀번호 재설정 이메일 전송 이벤트 발행
        eventPublisher.publishEvent(new PasswordResetEmailSendEvent(email, code));
    }

    // 2. 사용자가 입력한 6자리 인증 코드 확인 및 일회성 Reset Token 발급
    @Transactional(noRollbackFor = EmailException.class)
    public PasswordResetVerifyResponseDTO verifyPasswordResetCode(PasswordResetVerifyRequestDTO request) {
        String email = normalizeEmail(request.email());

        // 가장 최근의 인증 요청 조회
        EmailVerification verification = emailVerificationRepository.findTopByEmailOrderByEmailVerificationIdDesc(email)
                .orElseThrow(() -> new EmailException(EmailErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        // 검증 가능한 상태인지 확인 (만료, 횟수 초과, 이미 사용됨 등)
        validateVerifiable(verification);

        // 해시값 비교로 코드 일치 여부 확인
        if (!verification.getCodeHash().equals(hash(email, request.code()))) {
            verification.increaseAttemptCount();
            throw new EmailException(EmailErrorCode.EMAIL_CODE_MISMATCH);
        }

        // 검증 성공 시 일회성 비밀번호 재설정 보안 토큰 생성 (32바이트)
        String resetToken = generateResetToken();
        verification.markVerified(hash(email, resetToken));

        return new PasswordResetVerifyResponseDTO(resetToken);
    }

    // 3. Reset Token 검증 후 최종 비밀번호 재설정
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequestDTO request) {
        String email = normalizeEmail(request.email());

        if (request.resetToken() == null || request.resetToken().isBlank()) {
            throw new EmailException(EmailErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        // 사용되지 않은 유효한 비밀번호 재설정 토큰 조회
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndVerificationTokenHashAndConsumedAtIsNullOrderByEmailVerificationIdDesc(
                        email,
                        hash(email, request.resetToken())
                )
                .orElseThrow(() -> new EmailException(EmailErrorCode.EMAIL_VERIFICATION_INVALID));

        // 재설정 토큰의 소비 가능 상태 검증 (인증 여부, 만료, 소모 여부)
        validateConsumable(verification);

        // 대상 유저 조회 및 소셜 가입 여부 검증
        User user = getLocalUserByEmail(email);

        // 새 비밀번호 암호화 후 변경
        user.updatePassword(passwordEncoder.encode(request.newPassword()));

        // 계정 잠금 상태 및 실패 카운트 초기화
        user.resetFailedAttempts();

        // 기존 로그인된 모든 세션(Refresh Token) 강제 만료 처리
        refreshTokenRepository.deleteByUser(user);

        // 일회성 토큰 소모(Consumed) 처리
        verification.markConsumed();
    }


    // 로컬 회원 존재 여부 조회 및 소셜 전용 계정 변경 차단 검증
    private User getLocalUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        if (user.getSocialProvider() != AuthProvider.LOCAL) {
            throw new AuthException(AuthErrorCode.SOCIAL_USER_CANNOT_RESET_PASSWORD);
        }
        return user;
    }

    // 1분 이내 이메일 발송 요청 비율 제한 검증 (최대 3회)
    private void checkRateLimit(String email) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        int recentRequestCount = emailVerificationRepository.countByEmailAndCreatedAtAfter(email, oneMinuteAgo);
        if (recentRequestCount >= 3) {
            throw new AuthException(AuthErrorCode.TOO_MANY_REQUESTS);
        }
    }

    // 6자리 인증 코드 검증 가능 상태 확인 (만료, 최대 시도 횟수 초과, 소모 여부)
    private void validateVerifiable(EmailVerification verification) {
        if (verification.isExpired(LocalDateTime.now())) {
            throw new EmailException(EmailErrorCode.EMAIL_CODE_EXPIRED);
        }
        if (verification.getAttemptCount() >= MAX_VERIFY_ATTEMPTS) {
            throw new EmailException(EmailErrorCode.EMAIL_VERIFICATION_ATTEMPT_EXCEEDED);
        }
        if (verification.isConsumed()) {
            throw new EmailException(EmailErrorCode.EMAIL_VERIFICATION_INVALID);
        }
    }

    // 일회성 Reset Token 소비(사용) 가능 상태 확인 (인증 성공 여부, 만료, 소모 여부)
    private void validateConsumable(EmailVerification verification) {
        if (!verification.isVerified()) {
            throw new EmailException(EmailErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }
        if (verification.isExpired(LocalDateTime.now())) {
            throw new EmailException(EmailErrorCode.EMAIL_CODE_EXPIRED);
        }
        if (verification.isConsumed()) {
            throw new EmailException(EmailErrorCode.EMAIL_VERIFICATION_INVALID);
        }
    }

    // 6자리 랜덤 인증 코드 생성
    private String generateAuthCode() {
        return "%06d".formatted(secureRandom.nextInt(AUTH_CODE_BOUND));
    }

    // 32바이트 보안 랜덤 Reset Token 생성
    private String generateResetToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // SHA-256 + Pepper 조합 해싱
    private String hash(String email, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((email + ":" + value + ":" + pepper).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }

    // 이메일 공백 제거
    private String normalizeEmail(String email) {
        return email.strip();
    }
}
