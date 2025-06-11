package runrush.be.stats.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.stats.dto.MonthlyStats;
import runrush.be.stats.dto.PersonalBestStats;
import runrush.be.stats.dto.WeeklyStats;
import runrush.be.stats.service.RunningStatsService;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class RunningStatsController {
    private final RunningStatsService runningStatsService;

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyStats> getWeeklyStats(@AuthenticationPrincipal UserPrincipal user,
                                                      @RequestParam(required = false, defaultValue = "0") int weekOffset) {
        WeeklyStats stats = runningStatsService.getWeeklyStats(user.getId(), weekOffset);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyStats> getMonthStats(@AuthenticationPrincipal UserPrincipal user,
                                                      @RequestParam(required = false) Integer year,
                                                      @RequestParam(required = false) Integer month,
                                                      @RequestParam(required = false) Integer monthOffset) {
        MonthlyStats stats = runningStatsService.getMonthlyStats(user.getId(), year, month, monthOffset);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/personal-best")
    public ResponseEntity<PersonalBestStats> getPersonalBestStats(@AuthenticationPrincipal UserPrincipal user) {
        PersonalBestStats stats = runningStatsService.getPersonalBestStats(user.getId());
        return ResponseEntity.ok(stats);
    }
}