package runrush.be.recommendedCourse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.recommendedCourse.service.RecommendedCourseService;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class RecommendedCourseController {
    private final RecommendedCourseService recommendedCourseService;

    @PostMapping("/{recordId}")
    public ResponseEntity<Void> createCourse(@PathVariable Long recordId,
                                          @AuthenticationPrincipal UserPrincipal user) {

        recommendedCourseService.createRecommendedCourse(recordId, user.getEmail(), user.getName());
        return ResponseEntity.ok().build();
    }
}