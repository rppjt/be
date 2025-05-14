package runrush.be.user.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import runrush.be.auth.oauth2.dto.KakaoOAuth2UserInfo;
import runrush.be.user.domain.User;
import runrush.be.user.repository.UserRepository;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        Map<String, Object> attributes = super.loadUser(userRequest).getAttributes();
        log.debug("응답 구조: {}", attributes);

        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

        String email = userInfo.getEmail();
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "이메일 정보 제공에 동의해주세요."
            ));
        }

        User user = userRepository.findByKakaoId(userInfo.getId())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .kakaoId(userInfo.getId())
                            .email(userInfo.getEmail())
                            .name(userInfo.getName())
                            .profileImage(userInfo.getImage())
                            .build();
                    return userRepository.save(newUser);
                });

        log.info("카카오 로그인 성공: 사용자 ID = {}, 이메일 = {}", user.getId(), email);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
    }
}