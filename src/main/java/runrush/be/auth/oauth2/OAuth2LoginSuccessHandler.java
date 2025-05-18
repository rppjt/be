package runrush.be.auth.oauth2;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import runrush.be.auth.jwt.JwtTokenProvider;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.auth.service.RefreshTokenService;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 로그인 성공");

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String email = userPrincipal.getEmail();

        String accessToken = jwtTokenProvider.generateAccessToken(email);
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

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("accessToken", accessToken);
        responseBody.put("tokenType", "Bearer");
        responseBody.put("status", "success");

        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
        log.info("OAuth2 로그인 성공 처리 완료");
    }
}