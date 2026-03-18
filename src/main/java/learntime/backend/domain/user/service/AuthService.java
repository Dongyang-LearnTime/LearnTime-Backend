package learntime.backend.domain.user.service;

import learntime.backend.domain.user.dto.request.LoginRequest;
import learntime.backend.domain.user.model.RefreshToken;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.RefreshTokenRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.config.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    private static final long ACCESS_TIME = 1000L * 60 * 30; // 엑세스 토큰 유효기간, 30분
    private static final long REFRESH_TIME = 1000L * 60 * 60 * 24 * 14; // 리프레쉬 토큰 유효기간, 14일

    public record TokenPair(
            String accessToken,
            String refreshToken
    ) {}

    @Transactional
    public TokenPair login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return generateTokenPair(user);
    }
    // 토큰 검증
    @Transactional
    public TokenPair refresh(String refreshToken) {

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 리프레시 토큰입니다."));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new RuntimeException("리프레시 토큰이 만료되었습니다. 다시 로그인하세요.");
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
        String encodedPassword = passwordEncoder.encode(password); // 비밀번호 암호화

        User user = User.builder()
                .name(userName)
                .email(email)
                .password(encodedPassword)
                .role(User.Role.ROLE_ADMIN) // 관리자는 ROLE_ADMIN, 유저는 ROLE_USER
                .build();

        userRepository.save(user);
    }

    // 토큰 DB에 저장 및 return
    private TokenPair generateTokenPair(User user) {

        String newAccess = jwtProvider.createToken(user, ACCESS_TIME);
        String newRefresh = jwtProvider.createToken(user, REFRESH_TIME);

        refreshTokenRepository.deleteByUser(user); // 기존 리프레쉬 토큰 삭제

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(newRefresh)
                .expiryDate(LocalDateTime.now().plusSeconds(REFRESH_TIME / 1000))
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return new TokenPair(newAccess, newRefresh);
    }

}