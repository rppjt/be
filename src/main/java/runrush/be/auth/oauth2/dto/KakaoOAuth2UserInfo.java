package runrush.be.auth.oauth2.dto;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private Map<String, Object> attributes;
    private Map<String, Object> properties;
    private Map<String, Object> kakaoAccount;
    private Map<String, Object> profile;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.properties = (Map<String, Object>) attributes.get("properties");
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        if (this.kakaoAccount != null && this.kakaoAccount.containsKey("profile")) {
            this.profile = (Map<String, Object>) this.kakaoAccount.get("profile");
        }
    }

    @Override
    public String getId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getName() {
        return (String) profile.get("nickname");
    }

    @Override
    public String getEmail() {
        return (String) kakaoAccount.get("email");
    }

    @Override
    public String getImage() {
        return (String) profile.get("profile_image_url");
    }
}