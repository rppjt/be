package runrush.be.friends.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.friends.domain.FriendStatus;
import runrush.be.friends.domain.Friends;
import runrush.be.friends.dto.FriendResponse;
import runrush.be.friends.dto.ReceivedFriendRequestResponse;
import runrush.be.friends.dto.SentFriendRequestResponse;
import runrush.be.friends.repository.FriendsRepository;
import runrush.be.user.domain.User;
import runrush.be.user.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendsServiceTest {

    @Mock
    private FriendsRepository friendsRepository;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private FriendsService friendsService;

    @Test
    @DisplayName("친구 요청 전송 성공")
    void sendFriendRequest_Success() {
        Long requesterId = 1L;
        Long targetId = 2L;
        User requester = createTestUser(requesterId, "요청자");
        User target = createTestUser(targetId, "대상자");
        
        when(userService.findUserById(requesterId)).thenReturn(requester);
        when(userService.findUserById(targetId)).thenReturn(target);
        when(friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId)).thenReturn(Optional.empty());
        when(friendsRepository.findByRequesterIdAndTargetId(targetId, requesterId)).thenReturn(Optional.empty());

        assertThatCode(() -> friendsService.sendFriendRequest(requesterId, targetId))
                .doesNotThrowAnyException();

        verify(userService).findUserById(requesterId);
        verify(userService).findUserById(targetId);
        verify(friendsRepository).save(any(Friends.class));
    }

    @Test
    @DisplayName("친구 요청 전송 실패 - 자기 자신에게 요청")
    void sendFriendRequest_SelfRequest() {
        Long userId = 1L;

        assertThatThrownBy(() -> friendsService.sendFriendRequest(userId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_ADD_SELF_AS_FRIEND);

        verify(userService, never()).findUserById(anyLong());
        verify(friendsRepository, never()).save(any(Friends.class));
    }

    @Test
    @DisplayName("친구 요청 전송 실패 - 이미 요청 존재")
    void sendFriendRequest_AlreadyExists() {
        Long requesterId = 1L;
        Long targetId = 2L;
        User requester = createTestUser(requesterId, "요청자");
        User target = createTestUser(targetId, "대상자");
        Friends existingRequest = mock(Friends.class);
        
        when(userService.findUserById(requesterId)).thenReturn(requester);
        when(userService.findUserById(targetId)).thenReturn(target);
        when(friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId))
                .thenReturn(Optional.of(existingRequest));

        assertThatThrownBy(() -> friendsService.sendFriendRequest(requesterId, targetId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);

        verify(friendsRepository, never()).save(any(Friends.class));
    }

    @Test
    @DisplayName("친구 요청 전송 실패 - 반대 방향 요청 존재")
    void sendFriendRequest_ReverseRequestExists() {
        Long requesterId = 1L;
        Long targetId = 2L;
        User requester = createTestUser(requesterId, "요청자");
        User target = createTestUser(targetId, "대상자");
        Friends existingRequest = mock(Friends.class);
        
        when(userService.findUserById(requesterId)).thenReturn(requester);
        when(userService.findUserById(targetId)).thenReturn(target);
        when(friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId)).thenReturn(Optional.empty());
        when(friendsRepository.findByRequesterIdAndTargetId(targetId, requesterId))
                .thenReturn(Optional.of(existingRequest));

        assertThatThrownBy(() -> friendsService.sendFriendRequest(requesterId, targetId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);

        verify(friendsRepository, never()).save(any(Friends.class));
    }

    @Test
    @DisplayName("친구 요청 수락 성공")
    void acceptFriendRequest_Success() {
        Long requesterId = 1L;
        Long targetId = 2L;
        User requester = createTestUser(requesterId, "요청자");
        User target = createTestUser(targetId, "대상자");
        Friends pendingRequest = mock(Friends.class);
        
        when(pendingRequest.getStatus()).thenReturn(FriendStatus.PENDING);
        when(friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId))
                .thenReturn(Optional.of(pendingRequest));
        when(userService.findUserById(requesterId)).thenReturn(requester);
        when(userService.findUserById(targetId)).thenReturn(target);

        assertThatCode(() -> friendsService.acceptFriendRequest(requesterId, targetId))
                .doesNotThrowAnyException();

        verify(friendsRepository).delete(pendingRequest);
        verify(friendsRepository, times(2)).save(any(Friends.class));
    }

    @Test
    @DisplayName("친구 요청 수락 실패 - 요청 없음")
    void acceptFriendRequest_RequestNotFound() {
        Long requesterId = 1L;
        Long targetId = 2L;
        
        when(friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendsService.acceptFriendRequest(requesterId, targetId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_NOT_FOUND);

        verify(friendsRepository, never()).delete(any(Friends.class));
        verify(friendsRepository, never()).save(any(Friends.class));
    }

    @Test
    @DisplayName("친구 요청 수락 실패 - 이미 처리된 요청")
    void acceptFriendRequest_AlreadyProcessed() {
        Long requesterId = 1L;
        Long targetId = 2L;
        Friends processedRequest = mock(Friends.class);
        
        when(processedRequest.getStatus()).thenReturn(FriendStatus.ACCEPTED);
        when(friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId))
                .thenReturn(Optional.of(processedRequest));

        assertThatThrownBy(() -> friendsService.acceptFriendRequest(requesterId, targetId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED);

        verify(friendsRepository, never()).delete(any(Friends.class));
        verify(friendsRepository, never()).save(any(Friends.class));
    }

    @Test
    @DisplayName("친구 요청 거절 성공")
    void rejectFriendRequest_Success() {
        Long requesterId = 1L;
        Long targetId = 2L;
        Friends pendingRequest = mock(Friends.class);
        
        when(pendingRequest.getStatus()).thenReturn(FriendStatus.PENDING);
        when(friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId))
                .thenReturn(Optional.of(pendingRequest));

        assertThatCode(() -> friendsService.rejectFriendRequest(requesterId, targetId))
                .doesNotThrowAnyException();

        verify(friendsRepository).deleteFriendsRequest(requesterId, targetId);
    }

    @Test
    @DisplayName("친구 요청 거절 실패 - 요청 없음")
    void rejectFriendRequest_RequestNotFound() {
        Long requesterId = 1L;
        Long targetId = 2L;
        
        when(friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendsService.rejectFriendRequest(requesterId, targetId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_NOT_FOUND);

        verify(friendsRepository, never()).deleteFriendsRequest(anyLong(), anyLong());
    }

    @Test
    @DisplayName("친구 요청 거절 실패 - 이미 처리된 요청")
    void rejectFriendRequest_AlreadyProcessed() {
        Long requesterId = 1L;
        Long targetId = 2L;
        Friends processedRequest = mock(Friends.class);
        
        when(processedRequest.getStatus()).thenReturn(FriendStatus.ACCEPTED);
        when(friendsRepository.findByRequesterIdAndTargetId(requesterId, targetId))
                .thenReturn(Optional.of(processedRequest));

        assertThatThrownBy(() -> friendsService.rejectFriendRequest(requesterId, targetId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED);

        verify(friendsRepository, never()).deleteFriendsRequest(anyLong(), anyLong());
    }

    @Test
    @DisplayName("친구 관계 해제 성공")
    void removeFriend_Success() {
        Long userId = 1L;
        Long friendId = 2L;
        Friends friendship = mock(Friends.class);
        
        when(friendship.getStatus()).thenReturn(FriendStatus.ACCEPTED);
        when(friendsRepository.findByRequesterIdAndTargetId(userId, friendId))
                .thenReturn(Optional.of(friendship));

        assertThatCode(() -> friendsService.removeFriend(userId, friendId))
                .doesNotThrowAnyException();

        verify(friendsRepository).deleteAllFriends(userId, friendId);
    }

    @Test
    @DisplayName("친구 관계 해제 실패 - 친구 관계 아님")
    void removeFriend_NotFriends() {
        Long userId = 1L;
        Long friendId = 2L;
        
        when(friendsRepository.findByRequesterIdAndTargetId(userId, friendId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendsService.removeFriend(userId, friendId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_FRIENDS);

        verify(friendsRepository, never()).deleteAllFriends(anyLong(), anyLong());
    }

    @Test
    @DisplayName("친구 관계 해제 실패 - 수락되지 않은 관계")
    void removeFriend_NotAcceptedRelation() {
        Long userId = 1L;
        Long friendId = 2L;
        Friends pendingRequest = mock(Friends.class);
        
        when(pendingRequest.getStatus()).thenReturn(FriendStatus.PENDING);
        when(friendsRepository.findByRequesterIdAndTargetId(userId, friendId))
                .thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> friendsService.removeFriend(userId, friendId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_FRIENDS);

        verify(friendsRepository, never()).deleteAllFriends(anyLong(), anyLong());
    }

    @Test
    @DisplayName("내 친구 목록 조회 성공")
    void getMyFriends_Success() {
        Long userId = 1L;
        User friend1 = createTestUser(2L, "친구1");
        User friend2 = createTestUser(3L, "친구2");
        
        Friends friendship1 = mock(Friends.class);
        Friends friendship2 = mock(Friends.class);
        when(friendship1.getTarget()).thenReturn(friend1);
        when(friendship2.getTarget()).thenReturn(friend2);
        
        List<Friends> myFriends = List.of(friendship1, friendship2);
        when(friendsRepository.findMyFriends(userId)).thenReturn(myFriends);

        List<FriendResponse> result = friendsService.getMyFriends(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).friendId()).isEqualTo(2L);
        assertThat(result.get(0).name()).isEqualTo("친구1");
        assertThat(result.get(1).friendId()).isEqualTo(3L);
        assertThat(result.get(1).name()).isEqualTo("친구2");
        
        verify(friendsRepository).findMyFriends(userId);
    }

    @Test
    @DisplayName("내 친구 목록 조회 - 빈 목록")
    void getMyFriends_EmptyList() {
        Long userId = 1L;
        
        when(friendsRepository.findMyFriends(userId)).thenReturn(List.of());

        List<FriendResponse> result = friendsService.getMyFriends(userId);

        assertThat(result).isEmpty();
        verify(friendsRepository).findMyFriends(userId);
    }

    @Test
    @DisplayName("보낸 친구 요청 목록 조회 성공")
    void getSentFriendRequest_Success() {
        Long userId = 1L;
        User target1 = createTestUser(2L, "대상자1");
        User target2 = createTestUser(3L, "대상자2");
        
        Friends request1 = mock(Friends.class);
        Friends request2 = mock(Friends.class);
        when(request1.getTarget()).thenReturn(target1);
        when(request2.getTarget()).thenReturn(target2);
        
        List<Friends> sentRequests = List.of(request1, request2);
        when(friendsRepository.findFriendsRequest(userId)).thenReturn(sentRequests);

        List<SentFriendRequestResponse> result = friendsService.getSentFriendRequest(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).targetId()).isEqualTo(2L);
        assertThat(result.get(0).name()).isEqualTo("대상자1");
        assertThat(result.get(1).targetId()).isEqualTo(3L);
        assertThat(result.get(1).name()).isEqualTo("대상자2");
        
        verify(friendsRepository).findFriendsRequest(userId);
    }

    @Test
    @DisplayName("보낸 친구 요청 목록 조회 - 빈 목록")
    void getSentFriendRequest_EmptyList() {
        Long userId = 1L;
        
        when(friendsRepository.findFriendsRequest(userId)).thenReturn(List.of());

        List<SentFriendRequestResponse> result = friendsService.getSentFriendRequest(userId);

        assertThat(result).isEmpty();
        verify(friendsRepository).findFriendsRequest(userId);
    }

    @Test
    @DisplayName("받은 친구 요청 목록 조회 성공")
    void getReceivedFriendRequest_Success() {
        Long userId = 1L;
        User requester1 = createTestUser(2L, "요청자1");
        User requester2 = createTestUser(3L, "요청자2");
        
        Friends request1 = mock(Friends.class);
        Friends request2 = mock(Friends.class);
        when(request1.getRequester()).thenReturn(requester1);
        when(request2.getRequester()).thenReturn(requester2);
        
        List<Friends> receivedRequests = List.of(request1, request2);
        when(friendsRepository.findFriendsTarget(userId)).thenReturn(receivedRequests);

        List<ReceivedFriendRequestResponse> result = friendsService.getReceivedFriendRequest(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).requesterId()).isEqualTo(2L);
        assertThat(result.get(0).name()).isEqualTo("요청자1");
        assertThat(result.get(1).requesterId()).isEqualTo(3L);
        assertThat(result.get(1).name()).isEqualTo("요청자2");
        
        verify(friendsRepository).findFriendsTarget(userId);
    }

    @Test
    @DisplayName("받은 친구 요청 목록 조회 - 빈 목록")
    void getReceivedFriendRequest_EmptyList() {
        Long userId = 1L;
        
        when(friendsRepository.findFriendsTarget(userId)).thenReturn(List.of());

        List<ReceivedFriendRequestResponse> result = friendsService.getReceivedFriendRequest(userId);

        assertThat(result).isEmpty();
        verify(friendsRepository).findFriendsTarget(userId);
    }

    @Test
    @DisplayName("친구 서비스 의존성 검증")
    void verifyServiceDependencies() {
        assertThat(friendsRepository).isNotNull();
        assertThat(userService).isNotNull();
        assertThat(friendsService).isNotNull();
    }

    private User createTestUser(Long id, String nickname) {
        User user = User.builder()
                .email("test" + id + "@example.com")
                .name(nickname) // Service calls getName(), not getNickname()
                .nickname(nickname + "_nick")
                .profileImage("profile" + id + ".jpg")
                .build();
        
        // Set ID using reflection
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID", e);
        }
        
        return user;
    }
}