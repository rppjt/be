package runrush.be.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import runrush.be.auth.domain.AccessToken;
import runrush.be.auth.domain.CookieFactory;
import runrush.be.auth.jwt.JwtTokenProvider;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieFactory cookieFactory;
    
    @Value("${COOKIE_SECURE}")
    private boolean cookieSecure;
    
    @Value("${COOKIE_SAME_SITE}")
    private String cookieSameSite;

    public void logout(String accessTokenWithBearer, HttpServletResponse response) {
        AccessToken accessToken = AccessToken.from(accessTokenWithBearer);
        
        if (!jwtTokenProvider.validateToken(accessToken.getToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        
        String email = jwtTokenProvider.getEmailFromToken(accessToken.getToken());
        refreshTokenService.deleteRefreshToken(email);
        
        response.addCookie(cookieFactory.createLogoutCookie());
    }

    public String reissueAccessToken(String refreshToken) {
        return refreshTokenService.renewAccessToken(refreshToken);
    }

    public void setRefreshTokenCookie(String email, HttpServletResponse response) {
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);
        Instant jwtExpiration = jwtTokenProvider.getJwtExpiration(refreshToken);
        
        refreshTokenService.renewRefreshToken(email, refreshToken, jwtExpiration);
        
        ResponseCookie cookie = cookieFactory.createRefreshTokenCookie(
            refreshToken, jwtExpiration, cookieSecure, cookieSameSite);
        
        response.setHeader("Set-Cookie", cookie.toString());
    }

    public String generateAccessToken(String email) {
        return jwtTokenProvider.generateAccessToken(email);
    }
}