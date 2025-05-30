package runrush.be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.kakao.map.rest-api-key}")
    private String kakaoMapApiKey;

    @Value("${app.kakao.map.base-url}")
    private String kakaoMapBaseUrl;

    @Bean
    public WebClient kakaoMapClient() {
        return WebClient.builder()
                .baseUrl(kakaoMapBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakoAk " + kakaoMapApiKey)
                .build();
    }
}