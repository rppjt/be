package runrush.be.stats.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.stats.dto.MonthlyStats;
import runrush.be.stats.dto.WeeklyStats;
import runrush.be.stats.service.RunningStatsService;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class RunningStatsController {
    private final RunningStatsService runningStatsService;

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyStats> getWeeklyStats(@AuthenticationPrincipal UserPrincipal user) {
        WeeklyStats stats = runningStatsService.getWeeklyStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyStats> getCurrentMonthStats(@AuthenticationPrincipal UserPrincipal user) {
        MonthlyStats stats = runningStatsService.getCurrentMonthlyStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/monthly/last")
    public ResponseEntity<MonthlyStats> getLastMonthStats(@AuthenticationPrincipal UserPrincipal user) {
        MonthlyStats stats = runningStatsService.getLastMonthlyStats(user.getId());
        return ResponseEntity.ok(stats);
    }
}