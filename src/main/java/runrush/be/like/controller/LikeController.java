package runrush.be.like.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.like.service.LikeService;

@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    @PostMapping("/{courseId}")
    public ResponseEntity<Void> toggleLike(@PathVariable Long courseId,
                                           @AuthenticationPrincipal UserPrincipal user) {
        likeService.likeToggle(user.getId(), courseId);
        return ResponseEntity.noContent().build();
    }
}