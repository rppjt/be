package runrush.be.stats.dto;

public record MonthlyStatsResponse(
        int totalRuns,
        double totalDistance,
        long totalTime,
        double averagePace,
        double averageDistance,

        int activeDays,
        double longestRun,
        double fastestPace,

        int month,
        int year
) {
    public static MonthlyStatsResponse empty(int month, int year) {
        return new MonthlyStatsResponse(0, 0.0, 0L, 0.0, 0.0, 0, 0.0, 0.0, month, year);
    }
}