package learntime.backend.global.config.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import learntime.backend.domain.user.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    @Value("${JWT_KEY}")
    private String jwtKey;

    @Value("${JWT_ISSUER}")
    private String jwtIssuer;

    private SecretKey secretKey;

//    // Access Token: 30분, Refresh Token: 14일 (실무 기준)
//    private final long ACCESS_TOKEN_EXPIRE_TIME = 1000L * 60 * 30;
//    private final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 14;

    @PostConstruct
    protected void init() {
        // 보안을 위해 SecretKey 객체로 변환 (HMAC-SHA 알고리즘용)
        this.secretKey = Keys.hmacShaKeyFor(jwtKey.getBytes(StandardCharsets.UTF_8));
    }

    // 토큰 생성 (유저와 유효기간 매개변수로 받음)
    public String createToken(User user, long expirationMs) {
        return Jwts.builder()
                .header()
                .add("typ","JWT")
                .and()
                .subject(user.getEmail())
                .issuer(jwtIssuer)        // 발급자
                .issuedAt(new Date())   // 발급 시간
                .expiration(new Date(System.currentTimeMillis() + expirationMs)) // 만료 시간 설정
                .claim("userId", user.getUserId()) // 발급 유저의 id
                .claim("role", user.getRole()) // 발급 유저의 권한
                .signWith(secretKey)          // 서명
                .compact();
    }

    // claim 추출 및 유효성 검증
    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.", e);
            throw new AuthException(AuthErrorCode.INVALID_JWT_SIGNATURE);
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.", e);
            throw new AuthException(AuthErrorCode.EXPIRED_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.", e);
            throw new AuthException(AuthErrorCode.UNSUPPORTED_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 비어있습니다.", e);
            throw new AuthException(AuthErrorCode.EMPTY_JWT_CLAIM);
        }
    }

    // 토큰에서 userId 추출
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("userId", Long.class) : null;
    }

    // 토큰에서 email 추출
    public String getEmail(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    // 유효성 검증
    public boolean validateToken(String token) {
        return getClaims(token) != null;
    }
}
