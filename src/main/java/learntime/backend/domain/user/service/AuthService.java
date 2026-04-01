package learntime.backend.domain.user.service;

import learntime.backend.domain.user.dto.request.LoginRequestDTO;
import learntime.backend.domain.user.model.PromptQuota;
import learntime.backend.domain.user.model.RefreshToken;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.PromptQuotaRepository;
import learntime.backend.domain.user.repository.RefreshTokenRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.config.security.CustomPasswordEncoder;
import learntime.backend.global.config.security.jwt.JwtProvider;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PromptQuotaRepository promptQuotaRepository;
    private final JwtProvider jwtProvider;
    private final CustomPasswordEncoder customPasswordEncoder;

    private static final long ACCESS_TIME = 1000L * 60 * 30; // 엑세스 토큰 유효기간, 30분
    private static final long REFRESH_TIME = 1000L * 60 * 60 * 24 * 14; // 리프레쉬 토큰 유효기간, 14일

    public record TokenPair(
            String accessToken,
            String refreshToken
    ) {}

    @Transactional
    public TokenPair login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!customPasswordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        return generateTokenPair(user);
    }
    // 토큰 검증
    @Transactional
    public TokenPair refresh(String refreshToken) {

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
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
    public void createUser(String userName, String email, String password) {
        String encodedPassword = customPasswordEncoder.encode(password); // 비밀번호 암호화

        User user = User.builder()
                .name(userName)
                .email(email)
                .password(encodedPassword)
                .role(User.Role.ROLE_USER) // 관리자는 ROLE_ADMIN, 유저는 ROLE_USER
                .build();
        User savedUser = userRepository.save(user);

        // 프롬프트 할당량 생성
        PromptQuota quota = new PromptQuota(savedUser);
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