package runrush.be.stats.dto;

public record MonthlyStats(
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
    public static MonthlyStats empty(int month, int year) {
        return new MonthlyStats(0, 0.0, 0L, 0.0, 0.0, 0, 0.0, 0.0, month, year);
    }
}