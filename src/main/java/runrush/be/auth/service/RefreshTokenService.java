package runrush.be.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.auth.domain.RefreshToken;
import runrush.be.auth.jwt.JwtTokenProvider;
import runrush.be.auth.repository.RefreshTokenRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-token.expiration}")
    private long jwtRefreshTokenExpiration;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    private void verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요");
        }

        if(!jwtTokenProvider.validateToken(token.getToken())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("리프레시 토큰이 유효하지 않습니다. 다시 로그인해주세요.");
        }
    }

    public String renewAccessToken(String token) {
        RefreshToken refreshToken = findByToken(token)
                .orElseThrow(() -> new RuntimeException("리프레시 토큰이 존재하지 않습니다."));

        verifyExpiration(refreshToken);

        return jwtTokenProvider.generateAccessToken(refreshToken.getUserEmail());
    }

    @Transactional
    public void deleteRefreshToken(String userEmail) {
        refreshTokenRepository.deleteByUserEmail(userEmail);
    }
}
