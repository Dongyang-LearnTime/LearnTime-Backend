package learntime.backend.domain.user.service;

import learntime.backend.domain.user.dto.request.EmailVerificationConfirmRequestDTO;
import learntime.backend.domain.user.dto.request.EmailVerificationRequestDTO;
import learntime.backend.domain.user.dto.response.EmailVerificationResponseDTO;
import learntime.backend.domain.user.model.EmailVerification;
import learntime.backend.domain.user.repository.EmailVerificationRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.code.EmailErrorCode;
import learntime.backend.global.error.code.FileErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.exception.EmailException;
import learntime.backend.global.error.exception.FileException;
import learntime.backend.global.utils.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String SIGNUP_AUTH_TEMPLATE_PATH = "templates/email/signup-auth.html";
    private static final String SIGNUP_AUTH_SUBJECT = "[Learn-Time] 회원가입 이메일 인증";
    private static final int AUTH_CODE_BOUND = 1_000_000;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ApplicationEventPublisher eventPublisher;

    @Value("${security.pepper}")
    private String pepper;

    @Value("${mail.verification-expiration-minutes:10}")
    private int authCodeExpiresMinutes;

    // 인증 코드 발송
    @Transactional
    public void sendVerificationCode(EmailVerificationRequestDTO request) {
        String email = normalizeEmail(request.email());

        // [Rate Limit 로직] 1분 이내에 동일 이메일로 3회 이상 요청 시 차단
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        int recentRequestCount = emailVerificationRepository.countByEmailAndCreatedAtAfter(email, oneMinuteAgo);
        
        if (recentRequestCount >= 3) {
            throw new AuthException(AuthErrorCode.TOO_MANY_REQUESTS);
        }

        // 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.USER_EMAIL_DUPLICATED);
        }

        // 인증 코드 생성 및 해싱
        String code = generateAuthCode();
        String codeHash = hash(email, code);

        // 인증 정보 저장 (application.yml 설정된 시간 기준 만료)
        EmailVerification verification = EmailVerification.create(
                email,
                codeHash,
                LocalDateTime.now().plusMinutes(authCodeExpiresMinutes)
        );
        emailVerificationRepository.save(verification);

        // 이벤트 발행 후 비동기로 이메일 전송 (리스너에서 처리)
        eventPublisher.publishEvent(new learntime.backend.domain.user.event.EmailSendEvent(email, code));
    }

    // 사용자가 입력한 인증 코드 검증
    @Transactional(noRollbackFor = EmailException.class)
    public EmailVerificationResponseDTO verifyCode(EmailVerificationConfirmRequestDTO request) {
        String email = normalizeEmail(request.email());
        
        // 가장 최근의 인증 요청 조회
        EmailVerification verification = emailVerificationRepository.findTopByEmailOrderByEmailVerificationIdDesc(email)
                .orElseThrow(() -> new EmailException(EmailErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        // 검증 가능한 상태인지 확인 (만료, 횟수 초과, 이미 사용됨 등)
        validateVerifiable(verification);

        // 해시값 비교로 코드 일치 여부 확인
        if (!verification.getCodeHash().equals(hash(email, request.code()))) {
            verification.increaseAttemptCount(); // 실패 시 시도 횟수 증가
            throw new EmailException(EmailErrorCode.EMAIL_CODE_MISMATCH);
        }

        // 검증 성공 시 회원가입용 토큰 생성
        String rawToken = generateVerificationToken();
        verification.markVerified(hash(email, rawToken));

        return new EmailVerificationResponseDTO(rawToken);
    }

    // 회원가입 시 전달받은 토큰 검증
    @Transactional
    public void verifySignupToken(String email, String rawToken) {
        String normalizedEmail = normalizeEmail(email);

        // 토큰이 비어있는지 확인
        if (rawToken == null || rawToken.isBlank()) {
            throw new EmailException(EmailErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        // 사용되지 않은 유효한 인증 정보 조회
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndVerificationTokenHashAndConsumedAtIsNullOrderByEmailVerificationIdDesc(
                        normalizedEmail,
                        hash(normalizedEmail, rawToken)
                )
                .orElseThrow(() -> new EmailException(EmailErrorCode.EMAIL_VERIFICATION_INVALID));

        // 이미 인증된 상태인지 확인
        if (!verification.isVerified()) {
            throw new EmailException(EmailErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }
        // 만료 여부 재확인
        if (verification.isExpired(LocalDateTime.now())) {
            throw new EmailException(EmailErrorCode.EMAIL_CODE_EXPIRED);
        }
        // 이미 사용된 토큰인지 재확인
        if (verification.isConsumed()) {
            throw new EmailException(EmailErrorCode.EMAIL_VERIFICATION_INVALID);
        }

        // 검증 완료 후 토큰 사용 처리
        verification.markConsumed();
    }

    // 인증 가능한 상태인지 체크
    private void validateVerifiable(EmailVerification verification) {
        // 만료 시간 체크
        if (verification.isExpired(LocalDateTime.now())) {
            throw new EmailException(EmailErrorCode.EMAIL_CODE_EXPIRED);
        }
        // 최대 시도 횟수 체크
        if (verification.getAttemptCount() >= MAX_VERIFY_ATTEMPTS) {
            throw new EmailException(EmailErrorCode.EMAIL_VERIFICATION_ATTEMPT_EXCEEDED);
        }
        // 이미 사용된 인증인지 체크
        if (verification.isConsumed()) {
            throw new EmailException(EmailErrorCode.EMAIL_VERIFICATION_INVALID);
        }
    }

    // 6자리 랜덤 인증 코드 생성
    private String generateAuthCode() {
        return "%06d".formatted(secureRandom.nextInt(AUTH_CODE_BOUND));
    }

    // 회원가입용 보안 토큰 생성 (32바이트)
    private String generateVerificationToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // 이메일, 값, 페퍼를 조합하여 SHA-256 해시 생성
    private String hash(String email, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 보안을 위해 페퍼를 추가하여 해싱
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
