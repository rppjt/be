package runrush.be.stats.dto;

import java.time.LocalDate;

public record WeeklyStats(
        int totalRuns,
        double totalDistance,
        long totalTime,
        double averagePace,
        double averageDistance,
        LocalDate startOfWeek,
        LocalDate endOfWeek
) {
    public static WeeklyStats empty(LocalDate start, LocalDate end) {
        return new WeeklyStats(0, 0, 0, 0, 0, start, end);
    }
}