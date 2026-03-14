package learntime.backend.global.config.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;

public record CustomUserDetails(Long userId, String email, String role) implements UserDetails {

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        // role을 GrantedAuthority 형태로 변환하여 반환
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return ""; // JWT 인증 방식이므로 비밀번호는 비워둡니다.
    }

    @Override
    public @NonNull String getUsername() {
        return email; // Spring Security 규격상 username은 email로 매핑
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}