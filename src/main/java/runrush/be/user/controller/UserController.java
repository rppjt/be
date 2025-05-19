package runrush.be.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.user.domain.User;
import runrush.be.user.dto.UserInfoResponse;
import runrush.be.user.service.UserService;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal UserPrincipal user) {
        if(user == null) {
            return ResponseEntity.status(401).build();
        }

        User userByEmail = userService.findUserByEmail(user.getEmail());
        UserInfoResponse userInfoResponse = UserInfoResponse.fromEntity(userByEmail);
        return ResponseEntity.ok(userInfoResponse);
    }
}
