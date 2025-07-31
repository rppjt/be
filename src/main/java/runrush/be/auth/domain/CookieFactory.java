package runrush.be.auth.domain;

import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CookieFactory {
    
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/";

    public Cookie createLogoutCookie() {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath(COOKIE_PATH);
        cookie.setHttpOnly(true);
        return cookie;
    }

    public ResponseCookie createRefreshTokenCookie(
            String refreshToken, 
            Instant expiration, 
            boolean secure, 
            String sameSite) {
        
        long secondsUntilExpiration = expiration.getEpochSecond() - Instant.now().getEpochSecond();
        
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(secondsUntilExpiration)
                .sameSite(sameSite)
                .build();
    }
}