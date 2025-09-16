package runrush.be.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runrush.be.auth.domain.RefreshToken;
import runrush.be.auth.service.AuthService;
import runrush.be.auth.service.RefreshTokenService;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Authentication", description = "인증 관리 API")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = request.getHeader("Authorization");

        authService.logout(accessToken, response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰 만료 또는 유효하지 않음")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> reissue(HttpServletRequest request) {
        String refreshToken = getRefreshTokenFromCookie(request);
        String accessToken = authService.reissueAccessToken(refreshToken);

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "token_type", "Bearer"
        ));
    }

    @Operation(summary = "액세스 토큰 발급", description = "리프레시 토큰을 사용하여 액세스 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 발급 성공"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰 만료 또는 유효하지 않음")
    })
    @GetMapping("/token")
    public ResponseEntity<?> getAccessToken(HttpServletRequest request) {
        String refreshToken = getRefreshTokenFromCookie(request);

        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED, "유효하지 않은 리프레시 토큰입니다."));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED, "리프레시 토큰이 만료되었습니다.");
        }

        String accessToken = authService.generateAccessToken(token.getUserEmail());

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "token_type", "Bearer"
        ));
    }


    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh_token")) {
                    return cookie.getValue();
                }
            }
        }
        throw new BusinessException(ErrorCode.TOKEN_NOT_PROVIDED, "리프레시 토큰이 쿠키에 존재하지 않습니다.");
    }
}