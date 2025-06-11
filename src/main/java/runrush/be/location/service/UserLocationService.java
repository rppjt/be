package runrush.be.location.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.location.domain.UserLocation;
import runrush.be.location.dto.LocationSharingResponse;
import runrush.be.location.repository.UserLocationRepository;
import runrush.be.user.service.UserService;

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
            throw new BusinessException(ErrorCode.LOCATION_UPDATE_REQUIRED, "위치 정보가 없습니다. 먼저 위치를 업데이트해주세요.");
        }
    }
}