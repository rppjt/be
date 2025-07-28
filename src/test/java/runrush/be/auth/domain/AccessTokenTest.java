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
    @DisplayName("Bearer 토큰 파싱 성공 - JWT 형태의 토큰")
    void from_ValidJwtToken() {
        String jwtToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        AccessToken accessToken = AccessToken.from(jwtToken);

        assertThat(accessToken.getToken()).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
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

    @Test
    @DisplayName("빈 문자열이면 예외")
    void from_EmptyString_ThrowsException() {
        assertThatThrownBy(() -> AccessToken.from(""))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("Bearer만 있고 토큰이 없으면 빈 토큰 반환")
    void from_BearerOnly_ReturnsEmptyToken() {
        String tokenWithBearerOnly = "Bearer ";

        AccessToken accessToken = AccessToken.from(tokenWithBearerOnly);

        assertThat(accessToken.getToken()).isEmpty();
    }

    @Test
    @DisplayName("잘못된 접두사면 예외")
    void from_InvalidPrefix_ThrowsException() {
        String basicToken = "Basic abc123def456";
        String customToken = "Token abc123def456";

        assertThatThrownBy(() -> AccessToken.from(basicToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);

        assertThatThrownBy(() -> AccessToken.from(customToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("대소문자 구분 - bearer는 인식하지 않음")
    void from_CaseSensitive_ThrowsException() {
        String lowercaseBearer = "bearer abc123def456";

        assertThatThrownBy(() -> AccessToken.from(lowercaseBearer))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("공백이 포함된 토큰 처리")
    void from_TokenWithSpaces() {
        String tokenWithSpaces = "Bearer token with spaces";

        AccessToken accessToken = AccessToken.from(tokenWithSpaces);

        assertThat(accessToken.getToken()).isEqualTo("token with spaces");
    }

    @Test
    @DisplayName("매우 긴 토큰 처리")
    void from_VeryLongToken() {
        String longTokenPart = "a".repeat(1000);
        String longToken = "Bearer " + longTokenPart;

        AccessToken accessToken = AccessToken.from(longToken);

        assertThat(accessToken.getToken()).isEqualTo(longTokenPart);
        assertThat(accessToken.getToken().length()).isEqualTo(1000);
    }

    @Test
    @DisplayName("특수문자가 포함된 토큰 처리")
    void from_TokenWithSpecialCharacters() {
        String specialToken = "Bearer abc123-def456_ghi789.jkl012";

        AccessToken accessToken = AccessToken.from(specialToken);

        assertThat(accessToken.getToken()).isEqualTo("abc123-def456_ghi789.jkl012");
    }
}