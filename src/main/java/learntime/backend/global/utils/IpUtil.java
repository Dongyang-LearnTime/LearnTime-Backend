package learntime.backend.global.utils;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    private IpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {

        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {

            return xForwardedFor
                    .split(",")[0]
                    .trim();
        }

        return request.getRemoteAddr();
    }

}
