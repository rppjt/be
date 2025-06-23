package runrush.be.location.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.location.dto.LocationSharingRequest;
import runrush.be.location.dto.LocationSharingResponse;
import runrush.be.location.dto.LocationUpdateRequest;
import runrush.be.location.dto.NearbyFriendResponse;
import runrush.be.location.service.UserLocationService;

import java.util.List;

@Tag(name = "User Location", description = "사용자 위치 관리 API")
@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
public class UserLocationController {
    private final UserLocationService userLocationService;

    @Operation(summary = "위치 업데이트", description = "사용자의 현재 위치를 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "위치 업데이트 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping
    public ResponseEntity<Void> updateLocation(@AuthenticationPrincipal UserPrincipal user,
                                               @RequestBody LocationUpdateRequest request) {
        userLocationService.updateLocation(user.getId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "위치 공유 설정", description = "사용자의 위치 공유 여부를 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "위치 공유 설정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/sharing")
    public ResponseEntity<LocationSharingResponse> setLocationSharing(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody LocationSharingRequest request) {
        LocationSharingResponse response = userLocationService.setLocationSharing(user.getId(), request.isSharing());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "위치 공유 상태 조회", description = "사용자의 현재 위치 공유 상태를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "위치 공유 상태 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/sharing")
    public ResponseEntity<LocationSharingResponse> getLocationSharingStatus(@AuthenticationPrincipal UserPrincipal user) {
        LocationSharingResponse response = userLocationService.getLocationSharingStatus(user.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "근처 친구 조회", description = "지정된 반경 내의 친구 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "근처 친구 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyFriendResponse>> getNearbyFriends(
            @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "검색 반경 (단위: km)") @RequestParam(defaultValue = "0.5") Double radius) {

        List<NearbyFriendResponse> nearbyFriends = userLocationService.getNearbyFriends(user.getId(), radius);
        return ResponseEntity.ok(nearbyFriends);
    }
}