package runrush.be.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;

import static org.assertj.core.api.Assertions.*;

class AccessTokenTest {

    @Test
    @DisplayName("Bearer 토큰 파싱 성공 - 정상적인 토큰")
    void from_ValidBearerToken() {
        String bearerToken = "Bearer abc123def456ghi789";

        AccessToken accessToken = AccessToken.from(bearerToken);

        assertThat(accessToken.getToken()).isEqualTo("abc123def456ghi789");
    }

    @Test
    @DisplayName("Bearer 접두사 없으면 예외")
    void from_MissingBearerPrefix_ThrowsException() {
        String invalidToken = "abc123def456";

        assertThatThrownBy(() -> AccessToken.from(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("null 토큰이면 예외")
    void from_NullToken_ThrowsException() {
        assertThatThrownBy(() -> AccessToken.from(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }
}