package runrush.be.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.user.domain.User;
import runrush.be.user.dto.UserUpdateRequest;
import runrush.be.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void findUserByEmail_Success() {
        String email = "test@example.com";
        User mockUser = createTestUser(email, "테스트유저");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        User result = userService.findUserByEmail(email);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("이메일로 사용자 조회 실패 - 사용자 없음")
    void findUserByEmail_UserNotFound() {
        String email = "notfound@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserByEmail(email))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    void findUserById_Success() {
        Long userId = 1L;
        User mockUser = createTestUser("test@example.com", "테스트유저");
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        User result = userService.findUserById(userId);

        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("ID로 사용자 조회 실패 - 사용자 없음")
    void findUserById_UserNotFound() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserById(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("닉네임 사용 가능 여부 - 사용 가능")
    void isNicknameAvailable_Available() {
        String nickname = "새로운닉네임";
        when(userRepository.existsByNickname(nickname)).thenReturn(false);

        boolean result = userService.isNicknameAvailable(nickname);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("닉네임 사용 가능 여부 - 이미 사용중")
    void isNicknameAvailable_AlreadyExists() {
        String nickname = "기존닉네임";
        when(userRepository.existsByNickname(nickname)).thenReturn(true);

        boolean result = userService.isNicknameAvailable(nickname);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("사용자 프로필 업데이트 - 정상 처리")
    void updateUserProfile_Success() {
        Long userId = 1L;
        User user = createTestUser("test@example.com", "기존닉네임");
        UserUpdateRequest request = new UserUpdateRequest("새로운닉네임", "new-image.jpg");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("새로운닉네임")).thenReturn(false);

        assertThatCode(() -> userService.updateUserProfile(userId, request))
                .doesNotThrowAnyException();

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("사용자 프로필 업데이트 실패 - 사용자 없음")
    void updateUserProfile_UserNotFound() {
        Long userId = 999L;
        UserUpdateRequest request = new UserUpdateRequest("새로운닉네임", null);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserProfile(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 프로필 업데이트 실패 - 중복 닉네임")
    void updateUserProfile_DuplicateNickname() {
        Long userId = 1L;
        User user = createTestUser("test@example.com", "기존닉네임");
        UserUpdateRequest request = new UserUpdateRequest("중복닉네임", null);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("중복닉네임")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUserProfile(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_EXISTS);
    }

    private User createTestUser(String email, String nickname) {
        return User.builder()
                .email(email)
                .nickname(nickname)
                .build();
    }
}