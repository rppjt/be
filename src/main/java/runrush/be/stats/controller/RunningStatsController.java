package runrush.be.stats.controller;

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
import runrush.be.stats.dto.*;
import runrush.be.stats.service.RunningStatsService;

import java.util.List;

@Tag(name = "Running Stats", description = "러닝 통계 API")
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class RunningStatsController {
    private final RunningStatsService runningStatsService;

    @Operation(summary = "주간 통계 조회", description = "사용자의 주간 러닝 통계를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주간 통계 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/weekly")
    public ResponseEntity<WeeklyStatsResponse> getWeeklyStats(@AuthenticationPrincipal UserPrincipal user,
                                                              @Parameter(description = "주 오프셋 (0: 이번주, -1: 지난주)") @RequestParam(required = false, defaultValue = "0") int weekOffset) {
        WeeklyStatsResponse stats = runningStatsService.getWeeklyStats(user.getId(), weekOffset);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "월간 통계 조회", description = "사용자의 월간 러닝 통계를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "월간 통계 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyStatsResponse> getMonthStats(@AuthenticationPrincipal UserPrincipal user,
                                                              @Parameter(description = "연도") @RequestParam(required = false) Integer year,
                                                              @Parameter(description = "월") @RequestParam(required = false) Integer month,
                                                              @Parameter(description = "월 오프셋") @RequestParam(required = false) Integer monthOffset) {
        MonthlyStatsResponse stats = runningStatsService.getMonthlyStats(user.getId(), year, month, monthOffset);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "개인 베스트 기록 조회", description = "사용자의 개인 베스트 러닝 기록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "개인 베스트 기록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/personal-best")
    public ResponseEntity<PersonalBestStatsResponse> getPersonalBestStats(@AuthenticationPrincipal UserPrincipal user) {
        PersonalBestStatsResponse stats = runningStatsService.getPersonalBestStats(user.getId());
        return ResponseEntity.ok(stats);
    }


    @Operation(summary = "인기 추천 코스 TOP 10 조회", description = "가장 인기 있는 추천 코스 TOP 10을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인기 추천 코스 조회 성공")
    })
    @GetMapping("/popular-courses")
    public ResponseEntity<List<PopularRecommendedCourseResponse>> getPopularRecommendedCourses() {
        List<PopularRecommendedCourseResponse> stats = runningStatsService.getPopularRecommendedCourses();
        return ResponseEntity.ok(stats);
    }
}