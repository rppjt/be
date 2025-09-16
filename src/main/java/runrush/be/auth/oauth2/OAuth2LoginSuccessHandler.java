package runrush.be.auth.oauth2;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.auth.service.AuthService;
import runrush.be.common.exception.BusinessException;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String email = userPrincipal.getEmail();

            authService.setRefreshTokenCookie(email, response);

            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("success", true)
                    .build().toUriString();

            response.sendRedirect(targetUrl);

            log.info("OAuth2 로그인 성공 - 리다이렉트: {}", targetUrl);

        } catch (BusinessException e) {
            log.error("OAuth2 로그인 처리 중 비즈니스 예외 발생: 코드={}, 메시지={}", e.getErrorCode().getCode(), e.getMessage());
            handleAuthenticationError(response, "비즈니스 로직 오류");
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 예상치 못한 오류 발생", e);
            handleAuthenticationError(response, "로그인 처리 중 오류");
        }
    }

    private void handleAuthenticationError(HttpServletResponse response, String errorReason) throws IOException {
        String errorUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("success", false)
                .queryParam("error", errorReason)
                .build().toUriString();

        response.sendRedirect(errorUrl);
        log.info("OAuth2 로그인 실패 - 에러 리다이렉트: {}", errorUrl);
    }
}