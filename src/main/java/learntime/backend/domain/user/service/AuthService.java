package learntime.backend.domain.user.service;

import learntime.backend.domain.user.dto.request.LoginRequestDTO;
import learntime.backend.domain.user.dto.request.SignUpRequestDTO;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.enums.Terms;
import learntime.backend.domain.user.model.PromptQuotas;
import learntime.backend.domain.user.model.RefreshToken;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.model.UserTerms;
import learntime.backend.domain.user.repository.PromptQuotaRepository;
import learntime.backend.domain.user.repository.RefreshTokenRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.domain.user.repository.UserTermsRepository;
import learntime.backend.global.config.security.CustomPasswordEncoder;
import learntime.backend.global.config.security.jwt.JwtProvider;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
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

    private final JwtProvider jwtProvider;
    private final CustomPasswordEncoder customPasswordEncoder;

    private static final long ACCESS_TIME = 1000L * 60 * 30; // 엑세스 토큰 유효기간, 30분
    private static final long REFRESH_TIME = 1000L * 60 * 60 * 24 * 14; // 리프레쉬 토큰 유효기간, 14일

    private static final int MAX_PASSWORD_ATTEMPTS = 5; // 비밀번호 5회 제한

    @Value("${gemini.api.max-quota}") // 프롬프트 할당량
    private int maxQuota;

    public record TokenPair(
            String accessToken,
            String refreshToken
    ) {}

    @Transactional(noRollbackFor = {BadCredentialsException.class, LockedException.class})
    public TokenPair login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 계정 잠금 여부 확인
        if (user.isAccountLocked()) {
            throw new LockedException("비밀번호 5회 오류로 계정이 잠겼습니다.");
        }

        if (!customPasswordEncoder.matches(request.password(), user.getPassword())) {
            user.incrementFailedAttempts();

            // 증가시킨 횟수가 5회 이상이 되는 순간
            if (user.getFailedAttempts() >= MAX_PASSWORD_ATTEMPTS) {
                user.lockAccount(); // 계정 잠금 시간 기록

                throw new LockedException("비밀번호 5회 오류로 계정이 잠겼습니다. 30분 후 다시 시도해주세요.");
            }

            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        user.resetFailedAttempts(); // 성공 시 카운트 및 잠금 해제

        return generateTokenPair(user);
    }
    // 토큰 검증
    @Transactional
    public TokenPair refresh(String refreshToken) {

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new AuthException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        return generateTokenPair(storedToken.getUser());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken) // 리프레쉬 토큰 db에서 삭제
                .ifPresent(refreshTokenRepository::delete);
    }

    // 회원가입
    @Transactional
    public void createUser(SignUpRequestDTO signUpData) {
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

        User user = User.builder()
                .name(signUpData.userName())
                .email(signUpData.email())
                .password(encodedPassword)
                .role(Role.ROLE_USER) // 관리자는 ROLE_ADMIN, 유저는 ROLE_USER
                .build();
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
    }

    // 토큰 DB에 저장 및 return
    private TokenPair generateTokenPair(User user) {
        String newAccess = jwtProvider.createToken(user, ACCESS_TIME);
        String newRefresh = jwtProvider.createToken(user, REFRESH_TIME);
        LocalDateTime newExpiry = LocalDateTime.now().plusSeconds(REFRESH_TIME / 1000);

        refreshTokenRepository.upsertToken(user.getUserId(), newRefresh, newExpiry);

        return new TokenPair(newAccess, newRefresh);
    }

}