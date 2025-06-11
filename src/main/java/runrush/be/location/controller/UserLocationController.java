package runrush.be.location.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.location.dto.LocationSharingRequest;
import runrush.be.location.dto.LocationSharingResponse;
import runrush.be.location.dto.LocationUpdateRequest;
import runrush.be.location.service.UserLocationService;

@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
public class UserLocationController {
    private final UserLocationService userLocationService;

    @PatchMapping
    public ResponseEntity<Void> updateLocation(@AuthenticationPrincipal UserPrincipal user,
                                               @RequestBody LocationUpdateRequest request) {
        userLocationService.updateLocation(user.getId(), request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/sharing")
    public ResponseEntity<LocationSharingResponse> setLocationSharing(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody LocationSharingRequest request) {
        LocationSharingResponse response = userLocationService.setLocationSharing(user.getId(), request.isSharing());
        return ResponseEntity.ok(response);
    }
}