package runrush.be.runningrecord.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import runrush.be.kakao.client.KakaoMapApiClient;
import runrush.be.runningrecord.domain.LocationResolver;

@Component
@RequiredArgsConstructor
public class KakaoLocationResolver implements LocationResolver {
    
    private final KakaoMapApiClient kakaoMapApiClient;
    
    @Override
    public String resolveLocationName(double latitude, double longitude) {
        return kakaoMapApiClient.reverseGeocode(latitude, longitude);
    }
}