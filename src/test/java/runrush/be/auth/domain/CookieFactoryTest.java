package runrush.be.auth.domain;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class CookieFactoryTest {

    private final CookieFactory cookieFactory = new CookieFactory();

    @Test
    @DisplayName("로그아웃 쿠키 생성 - 즉시 만료되는 쿠키")
    void createLogoutCookie() {
        Cookie logoutCookie = cookieFactory.createLogoutCookie();

        assertThat(logoutCookie.getName()).isEqualTo("refresh_token");
        assertThat(logoutCookie.getValue()).isEmpty();
        assertThat(logoutCookie.getMaxAge()).isEqualTo(0); // 즉시 만료
        assertThat(logoutCookie.getPath()).isEqualTo("/");
        assertThat(logoutCookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("Refresh Token 쿠키 생성 - 기본 설정")
    void createRefreshTokenCookie_BasicSettings() {
        String refreshToken = "test_refresh_token_12345";
        Instant expiration = Instant.now().plusSeconds(3600); // 1시간 후 만료
        boolean secure = true;
        String sameSite = "Strict";

        ResponseCookie cookie = cookieFactory.createRefreshTokenCookie(
                refreshToken, expiration, secure, sameSite);

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo(refreshToken);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

}