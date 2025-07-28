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

    @Test
    @DisplayName("Refresh Token 쿠키 생성 - secure=false 설정")
    void createRefreshTokenCookie_NotSecure() {
        String refreshToken = "test_token";
        Instant expiration = Instant.now().plusSeconds(1800); // 30분 후
        boolean secure = false;
        String sameSite = "Lax";

        ResponseCookie cookie = cookieFactory.createRefreshTokenCookie(
                refreshToken, expiration, secure, sameSite);

        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
    }

    @Test
    @DisplayName("Refresh Token 쿠키 생성 - MaxAge 계산 확인")
    void createRefreshTokenCookie_MaxAgeCalculation() {
        String refreshToken = "token";
        Instant expiration = Instant.now().plusSeconds(7200); // 2시간 후
        boolean secure = true;
        String sameSite = "Strict";

        ResponseCookie cookie = cookieFactory.createRefreshTokenCookie(
                refreshToken, expiration, secure, sameSite);

        // MaxAge가 대략 2시간(7200초) 정도인지 확인
        long maxAge = cookie.getMaxAge().getSeconds();
        assertThat(maxAge).isBetween(7190L, 7200L);
    }

    @Test
    @DisplayName("Refresh Token 쿠키 생성 - 짧은 만료 시간")
    void createRefreshTokenCookie_ShortExpiration() {
        String refreshToken = "short_token";
        Instant expiration = Instant.now().plusSeconds(60); // 1분 후
        boolean secure = false;
        String sameSite = "None";

        ResponseCookie cookie = cookieFactory.createRefreshTokenCookie(
                refreshToken, expiration, secure, sameSite);

        long maxAge = cookie.getMaxAge().getSeconds();
        assertThat(maxAge).isBetween(50L, 60L); // 대략 1분
    }

    @Test
    @DisplayName("Refresh Token 쿠키 생성 - 긴 토큰값")
    void createRefreshTokenCookie_LongToken() {
        String longRefreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        Instant expiration = Instant.now().plusSeconds(3600);
        boolean secure = true;
        String sameSite = "Strict";

        ResponseCookie cookie = cookieFactory.createRefreshTokenCookie(
                longRefreshToken, expiration, secure, sameSite);

        assertThat(cookie.getValue()).isEqualTo(longRefreshToken);
        assertThat(cookie.getValue().length()).isGreaterThan(100);
    }

    @Test
    @DisplayName("Refresh Token 쿠키 생성 - 다양한 SameSite 값")
    void createRefreshTokenCookie_DifferentSameSiteValues() {
        String refreshToken = "token";
        Instant expiration = Instant.now().plusSeconds(3600);
        boolean secure = true;

        // Strict
        ResponseCookie strictCookie = cookieFactory.createRefreshTokenCookie(
                refreshToken, expiration, secure, "Strict");
        assertThat(strictCookie.getSameSite()).isEqualTo("Strict");

        // Lax
        ResponseCookie laxCookie = cookieFactory.createRefreshTokenCookie(
                refreshToken, expiration, secure, "Lax");
        assertThat(laxCookie.getSameSite()).isEqualTo("Lax");

        // None
        ResponseCookie noneCookie = cookieFactory.createRefreshTokenCookie(
                refreshToken, expiration, secure, "None");
        assertThat(noneCookie.getSameSite()).isEqualTo("None");
    }

    @Test
    @DisplayName("쿠키 생성 - 공통 속성 확인")
    void cookieCommonProperties() {
        String refreshToken = "test_token";
        Instant expiration = Instant.now().plusSeconds(3600);

        ResponseCookie cookie = cookieFactory.createRefreshTokenCookie(
                refreshToken, expiration, true, "Strict");

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isTrue(); // JavaScript에서 접근 불가
    }

    @Test
    @DisplayName("로그아웃 쿠키와 일반 쿠키의 차이점")
    void logoutVsRegularCookie() {
        Cookie logoutCookie = cookieFactory.createLogoutCookie();
        ResponseCookie refreshCookie = cookieFactory.createRefreshTokenCookie(
                "token", Instant.now().plusSeconds(3600), true, "Strict");

        assertThat(logoutCookie.getName()).isEqualTo(refreshCookie.getName());
        assertThat(logoutCookie.getValue()).isEmpty(); // 로그아웃은 빈 값
        assertThat(refreshCookie.getValue()).isNotEmpty(); // 일반은 토큰 값
        assertThat(logoutCookie.getMaxAge()).isEqualTo(0); // 로그아웃은 즉시 만료
        assertThat(refreshCookie.getMaxAge().getSeconds()).isGreaterThan(0); // 일반은 미래 만료
    }
}