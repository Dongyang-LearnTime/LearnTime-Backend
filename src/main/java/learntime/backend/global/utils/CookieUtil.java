package learntime.backend.global.utils;

import org.springframework.http.ResponseCookie;

public class CookieUtil {

    public static ResponseCookie createRefreshTokenCookie(String refreshToken, long refreshTime) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTime)
                .sameSite("Strict")
                .build();
    }

    public static ResponseCookie createEmptyRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }
}
