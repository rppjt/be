package runrush.be.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.user.domain.User;
import runrush.be.user.repository.UserRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token.expiration}")
    private long jwtAccessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long jwtRefreshTokenExpiration;

    private final UserRepository userRepository;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtAccessTokenExpiration);

        return Jwts.builder()
                .subject(email)
                .claim("userId", user.getId())
                .claim("name", user.getName())
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshTokenExpiration);

        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이라도 클레임은 읽을 수 있음 (토큰 타입 확인용)
            String tokenType = getTokenTypeFromExpiredToken(e);
            if ("refresh".equals(tokenType)) {
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
            } else {
                throw new BusinessException(ErrorCode.ACCESS_TOKEN_EXPIRED);
            }
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰: {}", e.getMessage());
            throw new BusinessException(ErrorCode.UNSUPPORTED_TOKEN);
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 토큰: {}", e.getMessage());
            throw new BusinessException(ErrorCode.MALFORMED_TOKEN);
        } catch (SignatureException e) {
            log.warn("JWT 서명이 유효하지 않음: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_SIGNATURE_INVALID);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있거나 null: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }

    public Instant getJwtExpiration(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().toInstant();
    }

    public boolean validateToken(String token) {
        if (token == null || !token.contains(".")) {
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("예상치 못한 토큰 검증 오류: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String getTokenTypeFromExpiredToken(ExpiredJwtException e) {
        try {
            return e.getClaims().get("tokenType", String.class);
        } catch (Exception ex) {
            log.warn("만료된 토큰에서 타입을 읽을 수 없음: {}", ex.getMessage());
            return "access";
        }
    }
}