package runrush.be.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import runrush.be.auth.domain.RefreshToken;
import runrush.be.auth.jwt.JwtTokenProvider;
import runrush.be.auth.repository.RefreshTokenRepository;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("토큰으로 RefreshToken 조회 성공")
    void findByToken_Success() {
        String token = "test-refresh-token";
        String userEmail = "test@example.com";

        RefreshToken mockRefreshToken = mock(RefreshToken.class);
        when(mockRefreshToken.getUserEmail()).thenReturn(userEmail);

        when(refreshTokenRepository.findByToken(token))
                .thenReturn(Optional.of(mockRefreshToken));

        Optional<RefreshToken> result = refreshTokenService.findByToken(token);

        assertThat(result).isPresent();
        assertThat(result.get().getUserEmail()).isEqualTo(userEmail);

        verify(refreshTokenRepository).findByToken(token);
    }

    @Test
    @DisplayName("토큰으로 RefreshToken 조회 실패 - 토큰 없음")
    void findByToken_NotFound() {
        String token = "test-refresh-token";

        when(refreshTokenRepository.findByToken(token))
                .thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenService.findByToken(token);

        assertThat(result).isEmpty();

        verify(refreshTokenRepository).findByToken(token);
    }

    @Test
    @DisplayName("액세스 토큰 갱신 성공")
    void renewAccessToken_Success() {
        String token = "test-new-token";
        String userEmail = "test@example.com";

        RefreshToken mockRefreshToken = mock(RefreshToken.class);
        when(mockRefreshToken.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(mockRefreshToken.getToken()).thenReturn(token);
        when(mockRefreshToken.getUserEmail()).thenReturn(userEmail);

        when(refreshTokenRepository.findByToken(token))
                .thenReturn(Optional.of(mockRefreshToken));

        when(jwtTokenProvider.generateAccessToken(userEmail))
                .thenReturn(token);
        when(jwtTokenProvider.validateToken(token))
                .thenReturn(true);

        String result = refreshTokenService.renewAccessToken(token);
        assertThat(result).isEqualTo(token);
        verify(refreshTokenRepository).findByToken(token);
    }

    @Test
    @DisplayName("액세스 토큰 갱신 실패 - 토큰 만료")
    void renewAccessToken_TokenExpired() {
        String token = "test-new-token";
        RefreshToken mockRefreshToken = mock(RefreshToken.class);
        when(mockRefreshToken.getExpiresAt()).thenReturn(Instant.now().minusSeconds(3600));

        when(refreshTokenRepository.findByToken(token))
                .thenReturn(Optional.of(mockRefreshToken));

        assertThatThrownBy(() -> refreshTokenService.renewAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_EXPIRED);

        verify(refreshTokenRepository).findByToken(token);
        verify(refreshTokenRepository).delete(mockRefreshToken);
    }

    @Test
    @DisplayName("리프레시 토큰 갱신 성공")
    void renewRefreshToken_Success() {
        String token = "test-new-token";
        String userEmail = "test@example.com";
        Instant expiresAt = Instant.now().plusSeconds(7200);

        assertThatCode(() -> refreshTokenService.renewRefreshToken(userEmail, token, expiresAt))
                .doesNotThrowAnyException();

        verify(refreshTokenRepository).deleteByUserEmail(userEmail);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("리프레시 토큰 삭제 성공")
    void deleteRefreshToken_Success() {
        String userEmail = "test@example.com";

        assertThatCode(() -> refreshTokenService.deleteRefreshToken(userEmail))
                .doesNotThrowAnyException();

        verify(refreshTokenRepository).deleteByUserEmail(userEmail);
    }

    @Test
    @DisplayName("액세스 토큰 갱신 실패 - 토큰 없음")
    void renewAccessToken_TokenNotFound() {
        String token = "invalid-token";

        when(refreshTokenRepository.findByToken(token))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.renewAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_EXPIRED);

        verify(refreshTokenRepository).findByToken(token);
    }

    @Test
    @DisplayName("액세스 토큰 갱신 실패 - JWT 토큰 검증 실패")
    void renewAccessToken_JwtValidationFailed() {
        String token = "invalid-jwt-token";

        RefreshToken mockRefreshToken = mock(RefreshToken.class);
        when(mockRefreshToken.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(mockRefreshToken.getToken()).thenReturn(token);

        when(refreshTokenRepository.findByToken(token))
                .thenReturn(Optional.of(mockRefreshToken));
        when(jwtTokenProvider.validateToken(token))
                .thenReturn(false);

        assertThatThrownBy(() -> refreshTokenService.renewAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);

        verify(refreshTokenRepository).findByToken(token);
        verify(refreshTokenRepository).delete(mockRefreshToken);
        verify(jwtTokenProvider).validateToken(token);
    }
}