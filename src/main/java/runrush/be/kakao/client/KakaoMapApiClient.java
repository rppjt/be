package runrush.be.kakao.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import runrush.be.kakao.dto.KakaoGeocodeResponse;

@Component
@RequiredArgsConstructor
public class KakaoMapApiClient {

    private final WebClient kakaoMapClient;

    @Value("${app.kakao.map.reverse-geocoding-path}")
    private String reverseGeocodingPath;

    public String reverseGeocode(double latitude, double longitude) {
        return kakaoMapClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(reverseGeocodingPath)
                        .queryParam("x", longitude)
                        .queryParam("y", latitude)
                        .build())
                .retrieve()
                .bodyToMono(KakaoGeocodeResponse.class)
                .map(KakaoGeocodeResponse::getFirstAddressName)
                .retry(3)
                .onErrorReturn("주소 정보 없음")
                .block();
    }
}