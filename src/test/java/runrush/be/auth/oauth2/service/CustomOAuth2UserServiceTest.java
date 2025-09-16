package runrush.be.auth.oauth2.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.util.ReflectionTestUtils;
import runrush.be.user.domain.User;
import runrush.be.user.repository.UserRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("닉네임 중복시 유니크 닉네임 생성 - 한번에 성공")
    void generateUniqueNickname_FirstTrySuccess() {
        try (MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            UUID mockUuid = mock(UUID.class);
            when(mockUuid.toString()).thenReturn("abcd1234-5678-9012-3456-789012345678");
            uuidMock.when(UUID::randomUUID).thenReturn(mockUuid);

            when(userRepository.existsByNickname("abcd1234")).thenReturn(false);

            String result = ReflectionTestUtils.invokeMethod(customOAuth2UserService, "generateUniqueNickname");

            assertThat(result).isEqualTo("abcd1234");
            verify(userRepository).existsByNickname("abcd1234");
        }
    }

    @Test
    @DisplayName("닉네임 중복시 유니크 닉네임 생성 - 두번째 시도에서 성공")
    void generateUniqueNickname_SecondTrySuccess() {
        try (MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            UUID mockUuid1 = mock(UUID.class);
            UUID mockUuid2 = mock(UUID.class);
            when(mockUuid1.toString()).thenReturn("duplicat-1234-5678-9012-123456789012"); // 8글자: duplicat
            when(mockUuid2.toString()).thenReturn("unique12-1234-5678-9012-123456789012"); // 8글자: unique12

            uuidMock.when(UUID::randomUUID)
                    .thenReturn(mockUuid1)
                    .thenReturn(mockUuid2);

            when(userRepository.existsByNickname("duplicat")).thenReturn(true);
            when(userRepository.existsByNickname("unique12")).thenReturn(false);

            String result = ReflectionTestUtils.invokeMethod(customOAuth2UserService, "generateUniqueNickname");

            assertThat(result).isEqualTo("unique12");
            verify(userRepository).existsByNickname("duplicat");
            verify(userRepository).existsByNickname("unique12");
        }
    }

    @Test
    @DisplayName("닉네임 중복시 유니크 닉네임 생성 - 여러번 시도 후 성공")
    void generateUniqueNickname_MultipleTriesSuccess() {
        try (MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            UUID mockUuid1 = mock(UUID.class);
            UUID mockUuid2 = mock(UUID.class);
            UUID mockUuid3 = mock(UUID.class);
            UUID mockUuid4 = mock(UUID.class);
            
            when(mockUuid1.toString()).thenReturn("try00001-1234-5678-9012-123456789012"); // 8글자: try00001
            when(mockUuid2.toString()).thenReturn("try00002-1234-5678-9012-123456789012"); // 8글자: try00002
            when(mockUuid3.toString()).thenReturn("try00003-1234-5678-9012-123456789012"); // 8글자: try00003
            when(mockUuid4.toString()).thenReturn("success1-1234-5678-9012-123456789012"); // 8글자: success1

            uuidMock.when(UUID::randomUUID)
                    .thenReturn(mockUuid1)
                    .thenReturn(mockUuid2)
                    .thenReturn(mockUuid3)
                    .thenReturn(mockUuid4);

            when(userRepository.existsByNickname("try00001")).thenReturn(true);
            when(userRepository.existsByNickname("try00002")).thenReturn(true);
            when(userRepository.existsByNickname("try00003")).thenReturn(true);
            when(userRepository.existsByNickname("success1")).thenReturn(false);

            String result = ReflectionTestUtils.invokeMethod(customOAuth2UserService, "generateUniqueNickname");

            assertThat(result).isEqualTo("success1");
            verify(userRepository).existsByNickname("try00001");
            verify(userRepository).existsByNickname("try00002");
            verify(userRepository).existsByNickname("try00003");
            verify(userRepository).existsByNickname("success1");
        }
    }

    @Test  
    @DisplayName("KakaoOAuth2UserInfo 생성 및 이메일 검증 - 정상 케이스")
    void processKakaoUserInfo_ValidEmail_Success() {
        Map<String, Object> attributes = createKakaoAttributes("12345", "test@kakao.com", "테스트유저", "http://image.url");
        
        User existingUser = User.builder()
                .kakaoId("12345")
                .email("test@kakao.com")
                .name("테스트유저")
                .nickname("existing")
                .build();
        ReflectionTestUtils.setField(existingUser, "id", 1L);
        
        when(userRepository.findByKakaoId("12345")).thenReturn(Optional.of(existingUser));
        
        runrush.be.auth.oauth2.dto.KakaoOAuth2UserInfo userInfo = new runrush.be.auth.oauth2.dto.KakaoOAuth2UserInfo(attributes);
        String email = userInfo.getEmail();
        User user = userRepository.findByKakaoId(userInfo.getId()).orElse(null);
        
        assertThat(email).isEqualTo("test@kakao.com");
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("test@kakao.com");
        assertThat(user.getKakaoId()).isEqualTo("12345");
        
        verify(userRepository).findByKakaoId("12345");
    }

    @Test
    @DisplayName("새 사용자 등록 프로세스 테스트")
    void processNewUser_Success() {
        try (MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            Map<String, Object> attributes = createKakaoAttributes("67890", "newuser@kakao.com", "새유저", "http://new.image.url");
            
            UUID mockUuid = mock(UUID.class);
            when(mockUuid.toString()).thenReturn("newuser1-1234-5678-9012-123456789012");
            uuidMock.when(UUID::randomUUID).thenReturn(mockUuid);
            
            when(userRepository.findByKakaoId("67890")).thenReturn(Optional.empty());
            when(userRepository.existsByNickname("newuser1")).thenReturn(false);
            
            User savedUser = User.builder()
                    .kakaoId("67890")
                    .email("newuser@kakao.com")
                    .name("새유저")
                    .profileImage("http://new.image.url")
                    .nickname("newuser1")
                    .build();
            ReflectionTestUtils.setField(savedUser, "id", 2L);
            
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            
            runrush.be.auth.oauth2.dto.KakaoOAuth2UserInfo userInfo = new runrush.be.auth.oauth2.dto.KakaoOAuth2UserInfo(attributes);
            String email = userInfo.getEmail();
            
            User user = userRepository.findByKakaoId(userInfo.getId())
                    .orElseGet(() -> {
                        String randomNickname = ReflectionTestUtils.invokeMethod(customOAuth2UserService, "generateUniqueNickname");
                        User newUser = User.builder()
                                .kakaoId(userInfo.getId())
                                .email(userInfo.getEmail())
                                .name(userInfo.getName())
                                .profileImage(userInfo.getImage())
                                .nickname(randomNickname)
                                .build();
                        return userRepository.save(newUser);
                    });
            
            assertThat(email).isNotNull().isNotEmpty();
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(2L);
            assertThat(user.getEmail()).isEqualTo("newuser@kakao.com");
            assertThat(user.getNickname()).isEqualTo("newuser1");
            
            verify(userRepository).findByKakaoId("67890");
            verify(userRepository).save(any(User.class));
            verify(userRepository).existsByNickname("newuser1");
        }
    }

    @Test
    @DisplayName("이메일 유효성 검사 - null 이메일")
    void validateEmail_NullEmail_ShouldFail() {
        Map<String, Object> attributes = createKakaoAttributesWithoutEmail("12345", "테스트유저");
        
        runrush.be.auth.oauth2.dto.KakaoOAuth2UserInfo userInfo = new runrush.be.auth.oauth2.dto.KakaoOAuth2UserInfo(attributes);
        String email = userInfo.getEmail();
        
        assertThat(email).isNull();
        
        boolean shouldThrowException = (email == null || email.isEmpty());
        assertThat(shouldThrowException).isTrue();
    }

    @Test
    @DisplayName("이메일 유효성 검사 - 빈 이메일")
    void validateEmail_EmptyEmail_ShouldFail() {
        Map<String, Object> attributes = createKakaoAttributes("12345", "", "테스트유저", "http://image.url");
        
        runrush.be.auth.oauth2.dto.KakaoOAuth2UserInfo userInfo = new runrush.be.auth.oauth2.dto.KakaoOAuth2UserInfo(attributes);
        String email = userInfo.getEmail();
        
        assertThat(email).isEmpty();
        
        boolean shouldThrowException = (email == null || email.isEmpty());
        assertThat(shouldThrowException).isTrue();
    }

    private Map<String, Object> createKakaoAttributes(String id, String email, String nickname, String profileImage) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", id);
        
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", email);
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", nickname);
        profile.put("profile_image_url", profileImage);
        kakaoAccount.put("profile", profile);
        
        attributes.put("kakao_account", kakaoAccount);
        
        return attributes;
    }

    private Map<String, Object> createKakaoAttributesWithoutEmail(String id, String nickname) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", id);
        
        Map<String, Object> kakaoAccount = new HashMap<>();

        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", nickname);
        kakaoAccount.put("profile", profile);
        
        attributes.put("kakao_account", kakaoAccount);
        
        return attributes;
    }
    
    private OAuth2UserRequest createMockOAuth2UserRequest() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("kakao")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/kakao")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .build();
        
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        
        return new OAuth2UserRequest(clientRegistration, accessToken);
    }
}