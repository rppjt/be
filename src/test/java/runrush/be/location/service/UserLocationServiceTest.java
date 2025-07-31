package runrush.be.location.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.location.domain.UserLocation;
import runrush.be.location.dto.LocationSharingResponse;
import runrush.be.location.dto.LocationUpdateRequest;
import runrush.be.location.dto.NearbyFriendResponse;
import runrush.be.location.repository.UserLocationRepository;
import runrush.be.user.domain.User;
import runrush.be.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLocationServiceTest {

    @Mock
    private UserLocationRepository userLocationRepository;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private UserLocationService userLocationService;

    @Test
    @DisplayName("위치 공유 활성화 성공")
    void setLocationSharing_EnableSharing_Success() {
        Long userId = 1L;
        Boolean isSharing = true;

        UserLocation existingLocation = mock(UserLocation.class);

        when(userLocationRepository.findByUserId(userId)).thenReturn(Optional.of(existingLocation));

        LocationSharingResponse result = userLocationService.setLocationSharing(userId, isSharing);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isSharing()).isTrue();

        verify(existingLocation).toggleSharing(isSharing);
        verify(userLocationRepository).save(existingLocation);
    }

    @Test
    @DisplayName("위치 공유 비활성화 성공")
    void setLocationSharing_DisableSharing_Success() {
        Long userId = 1L;
        Boolean isSharing = false;

        UserLocation existingLocation = mock(UserLocation.class);

        when(userLocationRepository.findByUserId(userId)).thenReturn(Optional.of(existingLocation));

        LocationSharingResponse result = userLocationService.setLocationSharing(userId, isSharing);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isSharing()).isFalse();

        verify(existingLocation).toggleSharing(isSharing);
        verify(userLocationRepository).save(existingLocation);
    }

    @Test
    @DisplayName("위치 공유 설정 실패 - 위치 정보 없음")
    void setLocationSharing_NoLocationData() {
        Long userId = 1L;
        Boolean isSharing = true;
        
        when(userLocationRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userLocationService.setLocationSharing(userId, isSharing))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOCATION_UPDATE_REQUIRED);

        verify(userLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("위치 업데이트 - 기존 위치 존재, 의미있는 이동")
    void updateLocation_ExistingLocation_SignificantMovement() {
        Long userId = 1L;
        LocationUpdateRequest request = new LocationUpdateRequest(37.5665, 126.9780);
        
        UserLocation existingLocation = mock(UserLocation.class);
        when(existingLocation.hasSignificantMovement(request.latitude(), request.longitude()))
                .thenReturn(true);
        
        when(userLocationRepository.findByUserId(userId))
                .thenReturn(Optional.of(existingLocation));

        userLocationService.updateLocation(userId, request);

        verify(existingLocation).updateLocation(request.latitude(), request.longitude());
        verify(userLocationRepository).save(existingLocation);
        verify(userService, never()).findUserById(anyLong());
    }

    @Test
    @DisplayName("위치 업데이트 - 기존 위치 존재, 미미한 이동")
    void updateLocation_ExistingLocation_MinorMovement() {
        Long userId = 1L;
        LocationUpdateRequest request = new LocationUpdateRequest(37.5665, 126.9780);
        
        UserLocation existingLocation = mock(UserLocation.class);
        when(existingLocation.hasSignificantMovement(request.latitude(), request.longitude()))
                .thenReturn(false);
        
        when(userLocationRepository.findByUserId(userId))
                .thenReturn(Optional.of(existingLocation));

        userLocationService.updateLocation(userId, request);

        verify(existingLocation, never()).updateLocation(anyDouble(), anyDouble());
        verify(userLocationRepository, never()).save(any());
        verify(userService, never()).findUserById(anyLong());
    }

    @Test
    @DisplayName("위치 업데이트 - 첫 위치 등록")
    void updateLocation_NewLocation() {
        Long userId = 1L;
        LocationUpdateRequest request = new LocationUpdateRequest(37.5665, 126.9780);
        
        User user = mock(User.class);
        
        when(userLocationRepository.findByUserId(userId))
                .thenReturn(Optional.empty());
        when(userService.findUserById(userId)).thenReturn(user);

        userLocationService.updateLocation(userId, request);

        verify(userService).findUserById(userId);
        verify(userLocationRepository).save(any(UserLocation.class));
    }

    @Test
    @DisplayName("주변 친구 조회 - 사용자 위치 없음")
    void getNearbyFriends_NoUserLocation() {
        Long userId = 1L;
        Double radiusKm = 1.0;
        
        when(userLocationRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        List<NearbyFriendResponse> result = userLocationService.getNearbyFriends(userId, radiusKm);

        assertThat(result).isEmpty();
        verify(userLocationRepository, never()).findFriendsWithActiveLocation(anyLong(), any());
    }

    @Test
    @DisplayName("주변 친구 조회 - 활성 친구 없음")
    void getNearbyFriends_NoActiveFriends() {
        Long userId = 1L;
        Double radiusKm = 1.0;
        
        UserLocation userLocation = mock(UserLocation.class);
        
        when(userLocationRepository.findByUserId(userId))
                .thenReturn(Optional.of(userLocation));
        when(userLocationRepository.findFriendsWithActiveLocation(eq(userId), any()))
                .thenReturn(List.of());

        List<NearbyFriendResponse> result = userLocationService.getNearbyFriends(userId, radiusKm);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("주변 친구 조회 성공")
    void getNearbyFriends_Success() {
        Long userId = 1L;
        Double radiusKm = 1.0;
        
        UserLocation userLocation = mock(UserLocation.class);
        UserLocation friendLocation = mock(UserLocation.class);
        User friend = mock(User.class);
        
        when(friend.getId()).thenReturn(2L);
        when(friend.getNickname()).thenReturn("친구");
        when(friend.getProfileImage()).thenReturn("profile.jpg");
        when(friendLocation.getUser()).thenReturn(friend);
        when(friendLocation.getLatitude()).thenReturn(37.5665);
        when(friendLocation.getLongitude()).thenReturn(126.9780);
        
        when(userLocationRepository.findByUserId(userId))
                .thenReturn(Optional.of(userLocation));
        when(userLocationRepository.findFriendsWithActiveLocation(eq(userId), any()))
                .thenReturn(List.of(friendLocation));
        when(userLocation.calculateDistance(37.5665, 126.9780))
                .thenReturn(0.5); // 500m 거리

        List<NearbyFriendResponse> result = userLocationService.getNearbyFriends(userId, radiusKm);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(2L);
        assertThat(result.get(0).nickname()).isEqualTo("친구");
        assertThat(result.get(0).distance()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("위치 공유 상태 조회 - 위치 정보 있음")
    void getLocationSharingStatus_WithLocationData() {
        Long userId = 1L;
        
        UserLocation userLocation = mock(UserLocation.class);
        when(userLocation.getIsSharing()).thenReturn(true);
        
        when(userLocationRepository.findByUserId(userId))
                .thenReturn(Optional.of(userLocation));

        LocationSharingResponse result = userLocationService.getLocationSharingStatus(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isSharing()).isTrue();
    }

    @Test
    @DisplayName("위치 공유 상태 조회 - 위치 정보 없음")
    void getLocationSharingStatus_NoLocationData() {
        Long userId = 1L;
        
        when(userLocationRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        LocationSharingResponse result = userLocationService.getLocationSharingStatus(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isSharing()).isFalse();
    }
}