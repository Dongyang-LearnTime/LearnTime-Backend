package learntime.backend.global.utils;

import org.springframework.http.ResponseCookie;

public class CookieUtil {

    private static final long REFRESH_TIME = 60 * 60 * 24 * 14; // 14일 (초 단위)

    public static ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(REFRESH_TIME)
                .sameSite("None")
                .build();
    }

    public static ResponseCookie createEmptyRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();
    }
}
