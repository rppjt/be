package runrush.be.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.auth.service.AuthService;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    private static final String REDIRECT_URI = "http://localhost:3000/auth/callback";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuth2LoginSuccessHandler, "redirectUri", REDIRECT_URI);
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - 정상적인 리다이렉트 처리")
    void onAuthenticationSuccess_Success() throws Exception {
        String userEmail = "test@kakao.com";
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getEmail()).thenReturn(userEmail);

        oAuth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        verify(authService).setRefreshTokenCookie(userEmail, response);
        verify(response).sendRedirect("http://localhost:3000/auth/callback?success=true");
    }

    @Test
    @DisplayName("OAuth2 로그인 처리 중 BusinessException 발생시 에러 리다이렉트")
    void onAuthenticationSuccess_BusinessException() throws Exception {
        String userEmail = "test@kakao.com";
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getEmail()).thenReturn(userEmail);
        
        doThrow(new BusinessException(ErrorCode.INVALID_TOKEN))
                .when(authService).setRefreshTokenCookie(userEmail, response);

        oAuth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        verify(authService).setRefreshTokenCookie(userEmail, response);
        verify(response).sendRedirect("http://localhost:3000/auth/callback?success=false&error=비즈니스 로직 오류");
    }

    @Test
    @DisplayName("OAuth2 로그인 처리 중 예상치 못한 예외 발생시 에러 리다이렉트")
    void onAuthenticationSuccess_UnexpectedException() throws Exception {
        String userEmail = "test@kakao.com";
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getEmail()).thenReturn(userEmail);
        
        doThrow(new RuntimeException("예상치 못한 오류"))
                .when(authService).setRefreshTokenCookie(userEmail, response);

        oAuth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        verify(authService).setRefreshTokenCookie(userEmail, response);
        verify(response).sendRedirect("http://localhost:3000/auth/callback?success=false&error=로그인 처리 중 오류");
    }

    @Test
    @DisplayName("Authentication Principal 타입 캐스팅 예외 처리")
    void onAuthenticationSuccess_ClassCastException() throws Exception {
        Object wrongPrincipal = "wrong_principal_type";
        when(authentication.getPrincipal()).thenReturn(wrongPrincipal);

        oAuth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        verify(authService, never()).setRefreshTokenCookie(anyString(), any());
        verify(response).sendRedirect("http://localhost:3000/auth/callback?success=false&error=로그인 처리 중 오류");
    }

    @Test
    @DisplayName("UserPrincipal에서 이메일 추출 실패시 예외 처리")
    void onAuthenticationSuccess_EmailExtractionFailure() throws Exception {
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getEmail()).thenThrow(new RuntimeException("이메일 추출 실패"));

        oAuth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        verify(authService, never()).setRefreshTokenCookie(anyString(), any());
        verify(response).sendRedirect("http://localhost:3000/auth/callback?success=false&error=로그인 처리 중 오류");
    }

}