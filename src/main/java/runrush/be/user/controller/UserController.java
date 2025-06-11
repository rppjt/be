package runrush.be.user.controller;

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

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal UserPrincipal user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        User userByEmail = userService.findUserById(user.getId());
        UserInfoResponse userInfoResponse = UserInfoResponse.fromEntity(userByEmail);
        return ResponseEntity.ok(userInfoResponse);
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Object>> checkNickname(@RequestParam String nickname) {
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

    @PatchMapping("/update")
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal UserPrincipal user,
                                              @RequestBody UserUpdateRequest request) {
        userService.updateUserProfile(user.getId(), request);
        return ResponseEntity.ok().build();
    }
}