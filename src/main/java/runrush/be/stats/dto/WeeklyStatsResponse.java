package runrush.be.stats.dto;

import java.time.LocalDate;

public record WeeklyStatsResponse(
        int totalRuns,
        double totalDistance,
        long totalTime,
        double averagePace,
        double averageDistance,
        LocalDate startOfWeek,
        LocalDate endOfWeek
) {
    public static WeeklyStatsResponse empty(LocalDate start, LocalDate end) {
        return new WeeklyStatsResponse(0, 0, 0, 0, 0, start, end);
    }
}