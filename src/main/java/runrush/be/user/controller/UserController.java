package runrush.be.user.controller;

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
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.user.domain.User;
import runrush.be.user.dto.UserInfoResponse;
import runrush.be.user.dto.UserUpdateRequest;
import runrush.be.user.service.UserService;

import java.util.Map;

@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "사용자 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal UserPrincipal user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        User userByEmail = userService.findUserById(user.getId());
        UserInfoResponse userInfoResponse = UserInfoResponse.fromEntity(userByEmail);
        return ResponseEntity.ok(userInfoResponse);
    }

    @Operation(summary = "닉네임 중복 확인", description = "입력한 닉네임의 사용 가능 여부를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용 가능한 닉네임"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임")
    })
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Object>> checkNickname(@Parameter(description = "확인할 닉네임") @RequestParam String nickname) {
        boolean isAvailable = userService.isNicknameAvailable(nickname);

        if (isAvailable) {
            return ResponseEntity.ok(Map.of(
                    "available", true,
                    "message", "사용 가능한 닉네임입니다."
            ));
        } else {
            return ResponseEntity.status(409).body(Map.of(
                    "available", false,
                    "message", "이미 사용 중인 닉네임입니다."
            ));
        }
    }

    @Operation(summary = "사용자 프로필 수정", description = "사용자의 프로필 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/update")
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal UserPrincipal user,
                                              @RequestBody UserUpdateRequest request) {
        userService.updateUserProfile(user.getId(), request);
        return ResponseEntity.ok().build();
    }
}