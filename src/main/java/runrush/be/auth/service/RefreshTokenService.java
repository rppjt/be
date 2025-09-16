package runrush.be.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.auth.domain.RefreshToken;
import runrush.be.auth.jwt.JwtTokenProvider;
import runrush.be.auth.repository.RefreshTokenRepository;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public String renewAccessToken(String token) {
        RefreshToken refreshToken = findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED));

        verifyExpiration(refreshToken);

        return jwtTokenProvider.generateAccessToken(refreshToken.getUserEmail());
    }

    @Transactional
    public void renewRefreshToken(String email, String token, Instant expiresAt) {
        refreshTokenRepository.deleteByUserEmail(email);

        RefreshToken refreshToken = RefreshToken.builder()
                .userEmail(email)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void deleteRefreshToken(String userEmail) {
        refreshTokenRepository.deleteByUserEmail(userEmail);
    }

    private void verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        if(!jwtTokenProvider.validateToken(token.getToken())) {
            refreshTokenRepository.delete(token);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}