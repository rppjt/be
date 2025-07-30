package runrush.be.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.user.domain.User;
import runrush.be.user.repository.UserRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET = "testSecretKeyForJwtTokenProviderTestingPurposes123456789";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION = 86400000L; // 24시간

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtAccessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtRefreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
    }

    @Test
    @DisplayName("액세스 토큰 생성 성공")
    void generateAccessToken_Success() {
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .name("테스트 유저")
                .nickname("테스트")
                .build();
        
        ReflectionTestUtils.setField(user, "id", 1L);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        String token = jwtTokenProvider.generateAccessToken(email);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT는 3부분으로 구성
        
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);
        
        assertThat(extractedEmail).isEqualTo(email);
        assertThat(extractedUserId).isEqualTo(1L);
    }

    @Test
    @DisplayName("액세스 토큰 생성 실패 - 사용자 없음")
    void generateAccessToken_UserNotFound() {
        String email = "notfound@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jwtTokenProvider.generateAccessToken(email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("리프레시 토큰 생성 성공")
    void generateRefreshToken_Success() {
        String email = "test@example.com";

        String token = jwtTokenProvider.generateRefreshToken(email);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
        
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 성공")
    void getEmailFromToken_Success() {
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .name("테스트 유저")
                .nickname("테스트")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        String token = jwtTokenProvider.generateAccessToken(email);

        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("토큰 유효성 검증 성공")
    void validateToken_Success() {
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .name("테스트 유저")
                .nickname("테스트")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        String token = jwtTokenProvider.generateAccessToken(email);

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("토큰 유효성 검증 실패 - null 토큰")
    void validateToken_NullToken() {
        String token = null;

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("토큰 유효성 검증 실패 - 잘못된 형식")
    void validateToken_InvalidFormat() {
        String token = "invalid.token.format";

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("토큰 유효성 검증 실패 - 잘못된 서명")
    void validateToken_InvalidSignature() {
        String wrongSecret = "wrongSecretKey123456789012345678901234567890";
        SecretKey wrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));
        
        String tokenWithWrongSignature = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(tokenWithWrongSignature))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("토큰에서 사용자 ID 추출 성공")
    void getUserIdFromToken_Success() {
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .name("테스트 유저")
                .nickname("테스트")
                .build();
        ReflectionTestUtils.setField(user, "id", 123L);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        String token = jwtTokenProvider.generateAccessToken(email);

        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(123L);
    }

    @Test
    @DisplayName("토큰 만료시간 추출 성공")
    void getJwtExpiration_Success() {
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .name("테스트 유저")
                .nickname("테스트")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        String token = jwtTokenProvider.generateAccessToken(email);

        Instant expiration = jwtTokenProvider.getJwtExpiration(token);

        assertThat(expiration).isAfter(Instant.now());
        assertThat(expiration).isBefore(Instant.now().plusSeconds(3700)); // 1시간 + 여유시간
    }

    @Test
    @DisplayName("만료된 토큰 파싱 실패")
    void parseClaims_ExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("test@example.com")
                .claim("type", "access")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2시간 전
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // 1시간 전 만료
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtTokenProvider.getEmailFromToken(expiredToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("잘못된 형식 토큰 파싱 실패")
    void parseClaims_MalformedToken() {
        String malformedToken = "not.a.valid.jwt.token";

        assertThatThrownBy(() -> jwtTokenProvider.getEmailFromToken(malformedToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MALFORMED_TOKEN);
    }
}