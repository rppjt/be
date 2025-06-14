package runrush.be.recommendedCourse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.recommendedCourse.dto.RecommendedCourseListResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseMyListResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseUpdateRequest;
import runrush.be.recommendedCourse.enums.SortType;
import runrush.be.recommendedCourse.service.RecommendedCourseService;

import java.util.List;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class RecommendedCourseController {
    private final RecommendedCourseService recommendedCourseService;

    @PostMapping("/{recordId}")
    public ResponseEntity<Void> createCourse(@PathVariable Long recordId,
                                             @AuthenticationPrincipal UserPrincipal user) {
        recommendedCourseService.createRecommendedCourse(recordId, user.getId(), user.getName());
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
        RecommendedCourseResponse recommendedCourse = recommendedCourseService.getRecommendedCourseDetail(courseId, user.getId());
        return ResponseEntity.ok(recommendedCourse);
    }

    @GetMapping("/my")
    public ResponseEntity<List<RecommendedCourseMyListResponse>> getMyRecommendedCourses(@AuthenticationPrincipal UserPrincipal user) {
        List<RecommendedCourseMyListResponse> myRecommendedCourses = recommendedCourseService.getMyRecommendedCourses(user.getId());
        return ResponseEntity.ok(myRecommendedCourses);
    }

    @GetMapping
    public ResponseEntity<List<RecommendedCourseListResponse>> getAllRecommendedCourses(
            @RequestParam(defaultValue = "LIKE") SortType sortType,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<RecommendedCourseListResponse> recommendedCourses = recommendedCourseService.getRecommendedCourses(sortType, user.getId());
        return ResponseEntity.ok(recommendedCourses);
    }
}