package runrush.be.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import runrush.be.auth.jwt.JwtTokenProvider;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    public void logout(String accessToken, HttpServletResponse response) {
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("유효한 토큰 형식이 아닙니다.");
        }

        String token = accessToken.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        String email = jwtTokenProvider.getEmailFromToken(token);

        refreshTokenService.deleteRefreshToken(email);

        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    public String reissueAccessToken(String refreshToken) {
        return refreshTokenService.renewAccessToken(refreshToken);
    }

    public void setRefreshTokenCookie(String email, HttpServletResponse response) {
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);
        Instant jwtExpiration = jwtTokenProvider.getJwtExpiration(refreshToken);
        long secondsUntilExpiration = jwtExpiration.getEpochSecond() - Instant.now().getEpochSecond();

        refreshTokenService.renewRefreshToken(email, refreshToken, jwtExpiration);

        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // 로컬
        refreshTokenCookie.setMaxAge((int) secondsUntilExpiration);
        response.addCookie(refreshTokenCookie);
    }

    public String generateAccessToken(String email) {
        return jwtTokenProvider.generateAccessToken(email);
    }
}