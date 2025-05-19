package runrush.be.auth.oauth2.dto;

public interface OAuth2UserInfo{
    String getId();
    String getName();
    String getEmail();
    String getImage();
}