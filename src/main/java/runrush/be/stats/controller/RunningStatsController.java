package runrush.be.stats.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.stats.dto.*;
import runrush.be.stats.service.RunningStatsService;

import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class RunningStatsController {
    private final RunningStatsService runningStatsService;

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyStatsResponse> getWeeklyStats(@AuthenticationPrincipal UserPrincipal user,
                                                              @RequestParam(required = false, defaultValue = "0") int weekOffset) {
        WeeklyStatsResponse stats = runningStatsService.getWeeklyStats(user.getId(), weekOffset);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyStatsResponse> getMonthStats(@AuthenticationPrincipal UserPrincipal user,
                                                              @RequestParam(required = false) Integer year,
                                                              @RequestParam(required = false) Integer month,
                                                              @RequestParam(required = false) Integer monthOffset) {
        MonthlyStatsResponse stats = runningStatsService.getMonthlyStats(user.getId(), year, month, monthOffset);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/personal-best")
    public ResponseEntity<PersonalBestStatsResponse> getPersonalBestStats(@AuthenticationPrincipal UserPrincipal user) {
        PersonalBestStatsResponse stats = runningStatsService.getPersonalBestStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * 특정 추천 코스의 상세 통계 조회
     */
    @GetMapping("/recommended-course/{courseId}")
    public ResponseEntity<RecommendedCourseDetailStatsResponse> getRecommendedCourseDetailStats(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal user) {
        RecommendedCourseDetailStatsResponse stats = runningStatsService.getRecommendedCourseDetailStats(courseId, user.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * 인기 추천 코스 TOP 10 조회
     */
    @GetMapping("/popular-courses")
    public ResponseEntity<List<PopularRecommendedCourseResponse>> getPopularRecommendedCourses() {
        List<PopularRecommendedCourseResponse> stats = runningStatsService.getPopularRecommendedCourses();
        return ResponseEntity.ok(stats);
    }
}