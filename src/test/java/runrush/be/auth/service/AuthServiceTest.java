package runrush.be.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;
import runrush.be.auth.domain.CookieFactory;
import runrush.be.auth.jwt.JwtTokenProvider;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private CookieFactory cookieFactory;
    
    @Mock
    private HttpServletResponse response;
    
    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로그아웃 성공 - 정상적인 토큰")
    void logout_Success() {
        String bearerToken = "Bearer valid-access-token";
        String email = "test@example.com";
        Cookie logoutCookie = new Cookie("refresh_token", "");
        
        when(jwtTokenProvider.validateToken("valid-access-token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("valid-access-token")).thenReturn(email);
        when(cookieFactory.createLogoutCookie()).thenReturn(logoutCookie);
        doNothing().when(refreshTokenService).deleteRefreshToken(email);
        
        assertThatCode(() -> authService.logout(bearerToken, response))
                .doesNotThrowAnyException();
        
        verify(jwtTokenProvider).validateToken("valid-access-token");
        verify(jwtTokenProvider).getEmailFromToken("valid-access-token");
        verify(refreshTokenService).deleteRefreshToken(email);
        verify(response).addCookie(logoutCookie);
    }
    
    @Test
    @DisplayName("로그아웃 실패 - 유효하지 않은 토큰")
    void logout_InvalidToken() {
        String bearerToken = "Bearer invalid-token";
        
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);
        
        assertThatThrownBy(() -> authService.logout(bearerToken, response))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
                
        verify(jwtTokenProvider, never()).getEmailFromToken(anyString());
        verify(refreshTokenService, never()).deleteRefreshToken(anyString());
        verify(response, never()).addCookie(any());
    }
    
    @Test
    @DisplayName("로그아웃 실패 - Bearer 접두어 없는 토큰")
    void logout_NoBearerPrefix() {
        String invalidBearerToken = "invalid-token-without-bearer";
        
        assertThatThrownBy(() -> authService.logout(invalidBearerToken, response))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
                
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }
    
    @Test
    @DisplayName("액세스 토큰 재발급 성공")
    void reissueAccessToken_Success() {
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";
        
        when(refreshTokenService.renewAccessToken(refreshToken)).thenReturn(newAccessToken);
        
        String result = authService.reissueAccessToken(refreshToken);
        
        assertThat(result).isEqualTo(newAccessToken);
        verify(refreshTokenService).renewAccessToken(refreshToken);
    }
    
    @Test
    @DisplayName("리프레시 토큰 쿠키 설정 성공")
    void setRefreshTokenCookie_Success() {
        String email = "test@example.com";
        String refreshToken = "new-refresh-token";
        Instant expiration = Instant.now().plusSeconds(3600);
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken).build();
        
        ReflectionTestUtils.setField(authService, "cookieSecure", true);
        ReflectionTestUtils.setField(authService, "cookieSameSite", "None");
        
        when(jwtTokenProvider.generateRefreshToken(email)).thenReturn(refreshToken);
        when(jwtTokenProvider.getJwtExpiration(refreshToken)).thenReturn(expiration);
        when(cookieFactory.createRefreshTokenCookie(refreshToken, expiration, true, "None")).thenReturn(cookie);
        doNothing().when(refreshTokenService).renewRefreshToken(email, refreshToken, expiration);
        
        assertThatCode(() -> authService.setRefreshTokenCookie(email, response))
                .doesNotThrowAnyException();

        verify(jwtTokenProvider).generateRefreshToken(email);
        verify(jwtTokenProvider).getJwtExpiration(refreshToken);
        verify(refreshTokenService).renewRefreshToken(email, refreshToken, expiration);
        verify(cookieFactory).createRefreshTokenCookie(refreshToken, expiration, true, "None");
        verify(response).setHeader("Set-Cookie", cookie.toString());
    }
    
    @Test
    @DisplayName("액세스 토큰 생성 성공")
    void generateAccessToken_Success() {
        String email = "test@example.com";
        String accessToken = "generated-access-token";
        
        when(jwtTokenProvider.generateAccessToken(email)).thenReturn(accessToken);
        
        String result = authService.generateAccessToken(email);
        
        assertThat(result).isEqualTo(accessToken);
        verify(jwtTokenProvider).generateAccessToken(email);
    }
    
    @Test
    @DisplayName("리프레시 토큰 쿠키 설정 - 비보안 모드")
    void setRefreshTokenCookie_UnsecureMode() {
        String email = "test@example.com";
        String refreshToken = "refresh-token";
        Instant expiration = Instant.now().plusSeconds(3600);
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken).build();
        
        ReflectionTestUtils.setField(authService, "cookieSecure", false);
        ReflectionTestUtils.setField(authService, "cookieSameSite", "Lax");
        
        when(jwtTokenProvider.generateRefreshToken(email)).thenReturn(refreshToken);
        when(jwtTokenProvider.getJwtExpiration(refreshToken)).thenReturn(expiration);
        when(cookieFactory.createRefreshTokenCookie(refreshToken, expiration, false, "Lax")).thenReturn(cookie);
        
        authService.setRefreshTokenCookie(email, response);
        
        verify(cookieFactory).createRefreshTokenCookie(refreshToken, expiration, false, "Lax");
    }
    
    @Test
    @DisplayName("로그아웃 - null 토큰 처리")
    void logout_NullToken() {
        String nullToken = null;
        
        assertThatThrownBy(() -> authService.logout(nullToken, response))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }
    
    @Test
    @DisplayName("로그아웃 - 빈 토큰 처리")
    void logout_EmptyToken() {
        String emptyToken = "";

        assertThatThrownBy(() -> authService.logout(emptyToken, response))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }
}