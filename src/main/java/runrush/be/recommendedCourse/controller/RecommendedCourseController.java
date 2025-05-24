package runrush.be.recommendedCourse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.recommendedCourse.dto.RecommendedCourseListResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseUpdateRequest;
import runrush.be.recommendedCourse.service.RecommendedCourseService;

import java.util.List;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class RecommendedCourseController {
    private final RecommendedCourseService recommendedCourseService;

    @PostMapping("/{courseId}")
    public ResponseEntity<Void> createCourse(@PathVariable Long courseId,
                                             @AuthenticationPrincipal UserPrincipal user) {
        recommendedCourseService.createRecommendedCourse(courseId, user.getId(), user.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<Void> updateCourse(@PathVariable Long courseId,
                                             @AuthenticationPrincipal UserPrincipal user,
                                             @RequestBody RecommendedCourseUpdateRequest request) {
        recommendedCourseService.updateRecommendedCourse(courseId, user.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId,
                                             @AuthenticationPrincipal UserPrincipal user) {
        recommendedCourseService.deleteRecommendedCourse(courseId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<RecommendedCourseResponse> getCourse(@PathVariable Long courseId,
                                                               @AuthenticationPrincipal UserPrincipal user) {
        RecommendedCourseResponse recommendedCourse = recommendedCourseService.getRecommendedCourse(courseId, user.getId());
        return ResponseEntity.ok().body(recommendedCourse);
    }

    @GetMapping("/user")
    public ResponseEntity<List<RecommendedCourseListResponse>> getUserRecommendedCourses(@AuthenticationPrincipal UserPrincipal user) {
        List<RecommendedCourseListResponse> userRecommendedCourses = recommendedCourseService.getUserRecommendedCourses(user.getId());
        return ResponseEntity.ok().body(userRecommendedCourses);
    }

    @GetMapping
    public ResponseEntity<List<RecommendedCourseListResponse>> getRecommendedCourses() {
        return ResponseEntity.ok().body(recommendedCourseService.getRecommendedCourses());
    }
}