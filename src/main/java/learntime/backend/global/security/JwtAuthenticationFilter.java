package learntime.backend.global.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.RefreshTokenRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request); // Authorization 헤더에서 토큰 추출

        // 토큰이 없거나, 이미 인증된 요청이면 다음 필터로 통과
        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtProvider.getClaims(token);

            if (claims != null) {
                // 토큰에서 데이터 추출
                String email = claims.getSubject();
                String name = claims.get("name", String.class); // 이름 추출
                String role = claims.get("role", String.class);
                Long userId = claims.get("userId", Long.class); // PK 추출

                // DB를 통한 추가 검증 (Redis 없이 토큰 무효화 제어)
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

                // 탈퇴한 사용자 접근 차단
                if (!user.getDeletedAt().equals(LocalDateTime.of(1970, 1, 1, 0, 0))) {
                    throw new AuthException(AuthErrorCode.DELETED_USER);
                }

                // 로그아웃된 토큰 차단 (RefreshToken이 없다면 로그아웃된 것으로 간주)
                if (!refreshTokenRepository.existsByUser_UserId(userId)) {
                    throw new AuthException(AuthErrorCode.INVALID_TOKEN);
                }

                // CustomUserDetails 객체 생성
                CustomUserDetails principal = new CustomUserDetails(userId, email, name, "", role, false);

                // Spring Security 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities() // CustomUserDetails에 구현된 메서드 활용
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("JWT Security Context 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response); // 다음 필터 진행
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
