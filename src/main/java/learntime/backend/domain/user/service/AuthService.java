package learntime.backend.domain.user.service;

import learntime.backend.domain.user.dto.request.LoginRequestDTO;
import learntime.backend.domain.user.dto.request.SignUpRequestDTO;
import learntime.backend.domain.user.enums.Terms;
import learntime.backend.domain.user.model.PromptQuotas;
import learntime.backend.domain.user.model.RefreshToken;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.model.UserTerms;
import learntime.backend.domain.user.converter.UserConverter;
import learntime.backend.domain.profile.enums.ProfileVisibility;
import learntime.backend.domain.profile.model.Profile;
import learntime.backend.domain.profile.repository.ProfileRepository;
import learntime.backend.domain.user.repository.PromptQuotaRepository;
import learntime.backend.domain.user.repository.RefreshTokenRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.domain.user.repository.UserTermsRepository;
import learntime.backend.global.security.CustomPasswordEncoder;
import learntime.backend.global.security.JwtProvider;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PromptQuotaRepository promptQuotaRepository;
    private final UserTermsRepository userTermsRepository;
    private final ProfileRepository profileRepository;

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final CustomPasswordEncoder customPasswordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Value("${jwt.access-expiration}")
    private long accessTime; // 엑세스 토큰 유효기간

    @Value("${jwt.refresh-expiration}")
    private long refreshTime; // 리프레쉬 토큰 유효기간

    private static final int MAX_PASSWORD_ATTEMPTS = 5; // 비밀번호 5회 제한

    @Value("${gemini.api.max-quota}") // 프롬프트 할당량
    private int maxQuota;

    public record TokenPair(
            String accessToken,
            String refreshToken
    ) {}

    @Transactional(noRollbackFor = {BadCredentialsException.class, LockedException.class, AuthException.class})
    public TokenPair login(LoginRequestDTO request) {
        User user = userRepository.findByEmailForUpdate(request.email())
                .orElseThrow(() -> new AuthException(AuthErrorCode.PASSWORD_NOT_MATCH));

        // 계정 잠금 확인
        if (user.isAccountLocked()) {
            throw new AuthException(AuthErrorCode.LOCKED_ACCOUNT);
        }
        try {
            // 인증 수행
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );

            user.resetFailedAttempts(); // 성공 시 실패 횟수 초기화

            return generateTokenPair(user);

        } catch (BadCredentialsException e) {
            user.incrementFailedAttempts(); // 실패 횟수 증가
            // 잠금 처리
            if (user.getFailedAttempts() >= MAX_PASSWORD_ATTEMPTS) {
                user.lockAccount();
                throw new AuthException(AuthErrorCode.LOCKED_ACCOUNT);
            }
            throw new AuthException(AuthErrorCode.PASSWORD_NOT_MATCH);
        }
    }

    // 토큰 검증
    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String hashedRefresh = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByToken(hashedRefresh)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new AuthException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        return generateTokenPair(storedToken.getUser());
    }

    @Transactional
    public void logout(String refreshToken) {
        String hashedRefresh = hashToken(refreshToken);
        refreshTokenRepository.findByToken(hashedRefresh) // 리프레쉬 토큰 db에서 삭제
                .ifPresent(refreshTokenRepository::delete);
    }

    // 회원가입
    @Transactional
    public void createUser(SignUpRequestDTO signUpData) {
        if (userRepository.existsByEmail(signUpData.email())) {
            throw new AuthException(AuthErrorCode.USER_EMAIL_DUPLICATED);
        }
        if (userRepository.existsByName(signUpData.userName())) {
            throw new AuthException(AuthErrorCode.USER_NAME_DUPLICATED);
        }

        emailVerificationService.verifySignupToken(signUpData.email(), signUpData.emailVerificationToken());

        // 필수 약관 동의 여부 확인
        for (Terms term : Terms.values()) {
            if (term.isRequired()) {
                // Map에서 해당 약관의 동의 여부를 확인, Map에 없으면 false로 간주
                boolean isAgreed = signUpData.termsAgreements().getOrDefault(term, false);
                if (!isAgreed) {
                    throw new AuthException(AuthErrorCode.TERMS_NOT_AGREED);
                }
            }
        }

        // 비밀번호 암호화
        String encodedPassword = customPasswordEncoder.encode(signUpData.password());

        User user = UserConverter.toUserEntity(signUpData, encodedPassword);
        User savedUser = userRepository.save(user);

        // 클라이언트가 보낸 약관 Map을 순회하며 UserTerms 엔티티 리스트 생성 후 저장
        List<UserTerms> userTermsList = signUpData.termsAgreements().entrySet().stream()
                .map(entry -> UserTerms.builder()
                        .user(savedUser)
                        .terms(entry.getKey())
                        .agreed(entry.getValue())
                        .agreedAt(LocalDateTime.now()) // 동의한 시점 기록
                        .build())
                .toList();
        userTermsRepository.saveAll(userTermsList);

        // 프롬프트 할당량 생성
        PromptQuotas quota = new PromptQuotas(savedUser, maxQuota);
        promptQuotaRepository.save(quota);

        // 기본 프로필 생성
        Profile profile = Profile.builder()
                .user(savedUser)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .build();
        profileRepository.save(profile);
    }

    // 토큰 DB에 저장 및 return
    public TokenPair generateTokenPair(User user) {
        String rawRefresh = jwtProvider.createToken(user, refreshTime);
        
        String hashedRefresh = hashToken(rawRefresh);
        
        String newAccess = jwtProvider.createAccessToken(user, accessTime, hashedRefresh);
        
        LocalDateTime newExpiry = LocalDateTime.now().plusSeconds(refreshTime / 1000);

        refreshTokenRepository.upsertToken(user.getUserId(), hashedRefresh, newExpiry);

        return new TokenPair(newAccess, rawRefresh);
    }

    // SHA-256 해시 함수
    private String hashToken(String token) {
        if (token == null) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

}
