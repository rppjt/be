package runrush.be.location.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLocationService {
    private final UserLocationRepository userLocationRepository;
    private final UserService userService;

    @Transactional
    public LocationSharingResponse setLocationSharing(Long userId, Boolean isSharing) {
        Optional<UserLocation> existingLocation = userLocationRepository.findById(userId);

        if (existingLocation.isPresent()) {
            UserLocation userLocation = existingLocation.get();

            userLocation.toggleSharing(isSharing);
            userLocationRepository.save(userLocation);

            log.info("사용자 {} 위치 공유 설정: {}", userId, isSharing);

            return isSharing
                    ? LocationSharingResponse.enabled(userId)
                    : LocationSharingResponse.disabled(userId);
        } else {
            throw new BusinessException(ErrorCode.LOCATION_UPDATE_REQUIRED);
        }
    }

    @Transactional
    public void updateLocation(Long userId, LocationUpdateRequest request) {
        Optional<UserLocation> existingLocation = userLocationRepository.findById(userId);

        if (existingLocation.isPresent()) {
            UserLocation userLocation = existingLocation.get();

            if (userLocation.hasSignificantMovement(request.latitude(), request.longitude())) {
                userLocation.updateLocation(request.latitude(), request.longitude());
                userLocationRepository.save(userLocation);
                log.info("사용자 {} 위치 업데이트: ({}, {})", userId, request.latitude(), request.longitude());
            } else {
                log.debug("사용자 {} 움직임이 미미하여 위치 업데이트 생략 (50m 미만)", userId);
            }
        } else {
            User user = userService.findUserById(userId);
            UserLocation newLocation = UserLocation.builder()
                    .user(user)
                    .latitude(request.latitude())
                    .longitude(request.longitude())
                    .isSharing(true) // 기본값: 위치 공유 허용
                    .build();

            userLocationRepository.save(newLocation);
            log.info("사용자 {} 첫 위치 등록: ({}, {})", userId, request.latitude(), request.longitude());
        }
    }

    @Transactional(readOnly = true)
    public List<NearbyFriendResponse> getNearbyFriends(Long userId, Double radiusKm) {
        Optional<UserLocation> userLocationOpt = userLocationRepository.findById(userId);
        if (userLocationOpt.isEmpty()) {
            log.debug("사용자 {} 위치 정보 없음, 빈 친구 목록 반환", userId);
            return List.of();
        }

        UserLocation userLocation = userLocationOpt.get();

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(10);
        List<UserLocation> friendLocations = userLocationRepository.findFriendsWithActiveLocation(userId, cutoffTime);

        log.debug("사용자 {} 주변 친구 조회: 활성 친구 {}명, 반경 {}km", userId, friendLocations.size(), radiusKm);

        if (friendLocations.isEmpty()) {
            log.debug("사용자 {} 주변에 활성 친구 없음", userId);
            return List.of();
        }

        return friendLocations.stream()
                .map(location -> {
                    double distance = userLocation.calculateDistance(
                            location.getLatitude(),
                            location.getLongitude()
                    );

                    return new NearbyFriendResponse(
                            location.getUser().getId(),
                            location.getUser().getNickname(),
                            location.getUser().getProfileImage(),
                            location.getLatitude(),
                            location.getLongitude(),
                            distance
                    );
                })
                .filter(friend -> friend.distance() <= radiusKm)
                .sorted((a, b) -> Double.compare(a.distance(), b.distance()))
                .peek(friend -> log.debug("주변 친구: {} ({}m 거리", friend.nickname(), Math.round(friend.distance() * 1000)))
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationSharingResponse getLocationSharingStatus(Long userId) {
        Optional<UserLocation> userLocation = userLocationRepository.findById(userId);

        if (userLocation.isPresent()) {
            Boolean currentStatus = userLocation.get().getIsSharing();
            log.debug("사용자 {} 위치 공유 상태 조회: {}", userId, currentStatus);
            return LocationSharingResponse.current(userId, currentStatus);
        } else {
            log.debug("사용자 {} 위치 정보 없음, 공유 상태: false", userId);
            return LocationSharingResponse.current(userId, false);
        }
    }
}