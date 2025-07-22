package runrush.be.auth.domain;

import lombok.Getter;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;

@Getter
public class AccessToken {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    
    private final String token;
    
    private AccessToken(String token) {
        this.token = token;
    }
    
    public static AccessToken from(String accessTokenWithBearer) {
        if (accessTokenWithBearer == null || !accessTokenWithBearer.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        
        String token = accessTokenWithBearer.substring(BEARER_PREFIX_LENGTH);
        return new AccessToken(token);
    }
}